package org.medix.service;

import com.stripe.StripeClient;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.medix.dto.CheckoutSessionRequest;
import org.medix.dto.PaymentStatusResponse;
import org.medix.dto.PaymentValidationResponse;
import org.medix.model.PaymentRecord;
import org.medix.repository.PaymentRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class PaymentService {

    private static final Logger LOG = Logger.getLogger(PaymentService.class);

    @Inject
    @ConfigProperty(name = "stripe.secret-key")
    String apiKey;

    @Inject
    @ConfigProperty(name = "stripe.webhook-secret")
    String webhookSecret;

    @Inject
    @ConfigProperty(name = "app.frontend.url", defaultValue = "http://localhost:5173")
    String frontendUrl;

    @Inject
    PaymentRepository paymentRepository;

    public Map<String, Object> createCheckout(CheckoutSessionRequest request) throws Exception {
        StripeClient client = new StripeClient(apiKey);

        String orderId = (request != null && request.orderId() != null && !request.orderId().isBlank())
                ? request.orderId()
                : "ORDER-" + System.currentTimeMillis();

        String productName = (request != null && request.productName() != null && !request.productName().isBlank())
                ? request.productName()
                : "POC Product";

        Long amount = (request != null && request.amount() != null && request.amount() > 0)
                ? request.amount()
                : 10000L;

        String currency = (request != null && request.currency() != null && !request.currency().isBlank())
                ? request.currency().toLowerCase()
                : "usd";

        String successUrl = (request != null && request.successUrl() != null && !request.successUrl().isBlank())
                ? request.successUrl()
                : frontendUrl + "/payment/loading?session_id={CHECKOUT_SESSION_ID}";

        String cancelUrl = (request != null && request.cancelUrl() != null && !request.cancelUrl().isBlank())
                ? request.cancelUrl()
                : frontendUrl + "/payment/cancel";

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("orderId", orderId)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency)
                                                .setUnitAmount(amount)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(productName)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = client.v1().checkout().sessions().create(params);

        // Record initial payment state in database as PENDING
        PaymentRecord initialRecord = new PaymentRecord(
                session.getId(),
                session.getId(),
                session.getPaymentIntent(),
                orderId,
                "PENDING",
                amount,
                currency,
                null,
                "card",
                Instant.now(),
                Instant.now()
        );
        paymentRepository.saveOrUpdate(initialRecord);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("checkoutUrl", session.getUrl());
        response.put("orderId", orderId);
        response.put("status", "PENDING");
        return response;
    }

    public void processWebhook(String payload, String signature) throws Exception {
        Event event = Webhook.constructEvent(payload, signature, webhookSecret);
        LOG.infof("Webhook received event type: %s", event.getType());

        switch (event.getType()) {
            case "checkout.session.completed":
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElseThrow();
                String paymentStatus = session.getPaymentStatus();
                String status = "paid".equalsIgnoreCase(paymentStatus) ? "PAID" : "COMPLETED";

                String customerEmail = null;
                if (session.getCustomerDetails() != null) {
                    customerEmail = session.getCustomerDetails().getEmail();
                }

                String orderId = null;
                if (session.getMetadata() != null) {
                    orderId = session.getMetadata().get("orderId");
                }

                PaymentRecord record = new PaymentRecord(
                        session.getId(),
                        session.getId(),
                        session.getPaymentIntent(),
                        orderId,
                        status,
                        session.getAmountTotal(),
                        session.getCurrency(),
                        customerEmail,
                        "card",
                        Instant.now(),
                        Instant.now()
                );
                paymentRepository.saveOrUpdate(record);
                LOG.infof("Webhook: Updated payment %s to status %s", session.getId(), status);
                break;

            case "checkout.session.expired":
                Session expiredSession = (Session) event.getDataObjectDeserializer().getObject().orElseThrow();
                paymentRepository.updateStatus(expiredSession.getId(), "EXPIRED");
                LOG.infof("Webhook: Payment %s marked as EXPIRED", expiredSession.getId());
                break;

            case "checkout.session.async_payment_succeeded":
                Session asyncSuccessSession = (Session) event.getDataObjectDeserializer().getObject().orElseThrow();
                paymentRepository.updateStatus(asyncSuccessSession.getId(), "PAID");
                LOG.infof("Webhook: Payment %s marked as PAID (async succeeded)", asyncSuccessSession.getId());
                break;

            case "checkout.session.async_payment_failed":
                Session asyncFailSession = (Session) event.getDataObjectDeserializer().getObject().orElseThrow();
                paymentRepository.updateStatus(asyncFailSession.getId(), "FAILED");
                LOG.infof("Webhook: Payment %s marked as FAILED (async failed)", asyncFailSession.getId());
                break;

            case "payment_intent.succeeded":
                PaymentIntent piSucceeded = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElseThrow();
                paymentRepository.updateStatus(piSucceeded.getId(), "PAID");
                LOG.infof("Webhook: PaymentIntent %s marked as PAID", piSucceeded.getId());
                break;

            case "payment_intent.payment_failed":
                PaymentIntent piFailed = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElseThrow();
                paymentRepository.updateStatus(piFailed.getId(), "FAILED");
                LOG.infof("Webhook: PaymentIntent %s marked as FAILED", piFailed.getId());
                break;

            default:
                LOG.infof("Webhook: Unhandled event type: %s", event.getType());
        }
    }

    public Optional<PaymentStatusResponse> getPaymentStatus(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .map(this::mapToStatusResponse);
    }

    public PaymentValidationResponse validatePayment(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) {
            return new PaymentValidationResponse(
                    paymentId,
                    "NOT_FOUND",
                    false,
                    "Payment ID must not be blank",
                    null,
                    null,
                    null
            );
        }

        // 1. Check local database record first
        Optional<PaymentRecord> existingOpt = paymentRepository.findByPaymentId(paymentId);
        if (existingOpt.isPresent()) {
            PaymentRecord existing = existingOpt.get();
            if ("PAID".equalsIgnoreCase(existing.getStatus()) || "SUCCESS".equalsIgnoreCase(existing.getStatus())) {
                return new PaymentValidationResponse(
                        paymentId,
                        "PAID",
                        true,
                        "Payment validated successfully from database records",
                        existing.getAmount(),
                        existing.getCurrency(),
                        existing.getCustomerEmail()
                );
            }
        }

        // 2. If status is pending or not in DB, verify directly against Stripe API
        try {
            StripeClient client = new StripeClient(apiKey);
            if (paymentId.startsWith("pi_")) {
                PaymentIntent pi = client.v1().paymentIntents().retrieve(paymentId);
                String piStatus = pi.getStatus();
                boolean isPaid = "succeeded".equalsIgnoreCase(piStatus);
                String status = isPaid ? "PAID" : piStatus.toUpperCase();

                paymentRepository.updateStatus(paymentId, status);
                return new PaymentValidationResponse(
                        paymentId,
                        status,
                        isPaid,
                        isPaid ? "Payment validated with Stripe" : "Payment status from Stripe is " + piStatus,
                        pi.getAmount(),
                        pi.getCurrency(),
                        pi.getReceiptEmail()
                );
            } else {
                Session session = client.v1().checkout().sessions().retrieve(paymentId);
                String paymentStatus = session.getPaymentStatus();
                boolean isPaid = "paid".equalsIgnoreCase(paymentStatus);
                String status = isPaid ? "PAID" : (session.getStatus() != null ? session.getStatus().toUpperCase() : "PENDING");

                String customerEmail = session.getCustomerDetails() != null ? session.getCustomerDetails().getEmail() : null;
                String orderId = session.getMetadata() != null ? session.getMetadata().get("orderId") : null;

                PaymentRecord updatedRecord = new PaymentRecord(
                        session.getId(),
                        session.getId(),
                        session.getPaymentIntent(),
                        orderId,
                        status,
                        session.getAmountTotal(),
                        session.getCurrency(),
                        customerEmail,
                        "card",
                        Instant.now(),
                        Instant.now()
                );
                paymentRepository.saveOrUpdate(updatedRecord);

                return new PaymentValidationResponse(
                        paymentId,
                        status,
                        isPaid,
                        isPaid ? "Payment validated with Stripe" : "Payment status from Stripe is " + paymentStatus,
                        session.getAmountTotal(),
                        session.getCurrency(),
                        customerEmail
                );
            }
        } catch (Exception e) {
            LOG.warnf("Direct Stripe validation failed for %s: %s", paymentId, e.getMessage());
        }

        // 3. Fallback to existing DB record if available
        if (existingOpt.isPresent()) {
            PaymentRecord existing = existingOpt.get();
            boolean isPaid = "PAID".equalsIgnoreCase(existing.getStatus());
            return new PaymentValidationResponse(
                    paymentId,
                    existing.getStatus(),
                    isPaid,
                    "Current status from database: " + existing.getStatus(),
                    existing.getAmount(),
                    existing.getCurrency(),
                    existing.getCustomerEmail()
            );
        }

        return new PaymentValidationResponse(
                paymentId,
                "NOT_FOUND",
                false,
                "Payment not found for ID: " + paymentId,
                null,
                null,
                null
        );
    }

    private PaymentStatusResponse mapToStatusResponse(PaymentRecord record) {
        return new PaymentStatusResponse(
                record.getId(),
                record.getSessionId(),
                record.getPaymentIntentId(),
                record.getOrderId(),
                record.getStatus(),
                record.getAmount(),
                record.getCurrency(),
                record.getCustomerEmail(),
                record.getPaymentMethod(),
                record.getUpdatedAt() != null ? record.getUpdatedAt().toString() : null
        );
    }
}
