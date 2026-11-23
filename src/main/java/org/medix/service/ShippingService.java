package org.medix.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.medix.dto.shipping.CreateShipmentRequest;
import org.medix.dto.shipping.ShippingAddress;
import org.medix.dto.shipping.ShippingRateOption;
import org.medix.dto.shipping.ShippingRateRequest;
import org.medix.dto.shipping.ShippingRateResponse;
import org.medix.dto.shipping.TrackingCheckpoint;
import org.medix.dto.shipping.TrackingResponse;
import org.medix.model.ShipmentRecord;
import org.medix.repository.ShipmentRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@ApplicationScoped
public class ShippingService {

    private static final Logger LOG = Logger.getLogger(ShippingService.class);
    private final Random random = new Random();

    @Inject
    ShipmentRepository shipmentRepository;

    public ShippingRateResponse calculateRates(ShippingRateRequest request) {
        long subtotal = (request != null && request.subtotal() != null) ? request.subtotal() : 0L;
        long standardCost = subtotal >= 10000L ? 0L : 500L; // Free standard shipping over $100

        List<ShippingRateOption> options = List.of(
                new ShippingRateOption(
                        "standard",
                        "Standard Ground Delivery",
                        "USPS Ground Advantage",
                        "3-5 business days",
                        standardCost,
                        standardCost == 0L ? "Free shipping on orders over $100" : "Economical ground shipping"
                ),
                new ShippingRateOption(
                        "express",
                        "Express 2-Day Air",
                        "FedEx 2Day",
                        "2 business days",
                        1500L,
                        "Fast and reliable delivery within 2 business days"
                ),
                new ShippingRateOption(
                        "priority",
                        "Priority Overnight",
                        "UPS Next Day Air",
                        "Next business day",
                        2500L,
                        "Guaranteed next business day delivery by 10:30 AM"
                )
        );

        return new ShippingRateResponse(options);
    }

    public ShipmentRecord createShipment(CreateShipmentRequest request) {
        String orderId = (request != null && request.orderId() != null) ? request.orderId() : "ORD-" + System.currentTimeMillis();
        String paymentId = (request != null && request.paymentId() != null) ? request.paymentId() : null;
        String method = (request != null && request.shippingMethod() != null) ? request.shippingMethod().toLowerCase() : "standard";

        String carrier;
        int deliveryDays;
        String trackingPrefix;

        switch (method) {
            case "express":
                carrier = "FedEx";
                deliveryDays = 2;
                trackingPrefix = "FDX-";
                break;
            case "priority":
                carrier = "UPS";
                deliveryDays = 1;
                trackingPrefix = "UPS-";
                break;
            case "standard":
            default:
                carrier = "USPS";
                deliveryDays = 4;
                trackingPrefix = "USPS-";
                break;
        }

        String trackingNumber = trackingPrefix + (100000000L + (long) (random.nextDouble() * 900000000L));
        Instant now = Instant.now();
        Instant estimatedDelivery = now.plus(deliveryDays, ChronoUnit.DAYS);

        ShippingAddress addr = request != null ? request.address() : null;
        String recipient = addr != null && addr.recipientName() != null ? addr.recipientName() : "Valued Customer";
        String street = addr != null && addr.street() != null ? addr.street() : "100 Market St";
        String city = addr != null && addr.city() != null ? addr.city() : "San Francisco";
        String state = addr != null && addr.state() != null ? addr.state() : "CA";
        String postalCode = addr != null && addr.postalCode() != null ? addr.postalCode() : "94105";
        String country = addr != null && addr.country() != null ? addr.country() : "US";

        String labelUrl = "/api/shipping/label/" + trackingNumber;

        ShipmentRecord record = new ShipmentRecord(
                UUID.randomUUID().toString(),
                orderId,
                paymentId,
                trackingNumber,
                carrier,
                method,
                "LABEL_CREATED",
                recipient,
                street,
                city,
                state,
                postalCode,
                country,
                labelUrl,
                estimatedDelivery,
                now,
                now
        );

        shipmentRepository.saveOrUpdate(record);
        LOG.infof("Created sandbox shipment %s for order %s (carrier: %s, method: %s)", trackingNumber, orderId, carrier, method);
        return record;
    }

    public Optional<TrackingResponse> getTracking(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber)
                .map(this::mapToTrackingResponse);
    }

    public Optional<TrackingResponse> getTrackingByOrderId(String orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .map(this::mapToTrackingResponse);
    }

    public Optional<TrackingResponse> getTrackingByPaymentId(String paymentId) {
        return shipmentRepository.findByPaymentId(paymentId)
                .map(this::mapToTrackingResponse);
    }

    public Optional<TrackingResponse> advanceTrackingStatus(String trackingNumber) {
        Optional<ShipmentRecord> recordOpt = shipmentRepository.findByTrackingNumber(trackingNumber);
        if (recordOpt.isEmpty()) {
            return Optional.empty();
        }

        ShipmentRecord record = recordOpt.get();
        String current = record.getStatus();
        String nextStatus;

        switch (current) {
            case "ORDER_PLACED":
                nextStatus = "LABEL_CREATED";
                break;
            case "LABEL_CREATED":
                nextStatus = "IN_TRANSIT";
                break;
            case "IN_TRANSIT":
                nextStatus = "OUT_FOR_DELIVERY";
                break;
            case "OUT_FOR_DELIVERY":
                nextStatus = "DELIVERED";
                break;
            case "DELIVERED":
            default:
                nextStatus = "DELIVERED";
                break;
        }

        shipmentRepository.updateStatus(trackingNumber, nextStatus);
        record.setStatus(nextStatus);
        LOG.infof("Sandbox shipment %s advanced from %s to %s", trackingNumber, current, nextStatus);
        return Optional.of(mapToTrackingResponse(record));
    }

    public ShipmentRecord autoCreateShipmentIfMissing(String orderId, String paymentId, String customerEmail, String shippingMethod, ShippingAddress address) {
        if (orderId != null) {
            Optional<ShipmentRecord> existing = shipmentRepository.findByOrderId(orderId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        if (paymentId != null) {
            Optional<ShipmentRecord> existing = shipmentRepository.findByPaymentId(paymentId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        ShippingAddress addr = address != null ? address : new ShippingAddress(
                customerEmail != null ? customerEmail.split("@")[0] : "Customer",
                "123 Tech Boulevard",
                "San Jose",
                "CA",
                "95113",
                "US"
        );

        CreateShipmentRequest req = new CreateShipmentRequest(
                orderId,
                paymentId,
                shippingMethod != null ? shippingMethod : "standard",
                addr,
                "E-Commerce Electronics Order"
        );
        return createShipment(req);
    }

    private TrackingResponse mapToTrackingResponse(ShipmentRecord r) {
        List<TrackingCheckpoint> checkpoints = new ArrayList<>();
        Instant baseTime = r.getCreatedAt() != null ? r.getCreatedAt() : Instant.now();

        checkpoints.add(new TrackingCheckpoint(
                "ORDER_PLACED",
                "Order received and sent to fulfillment center",
                "Fulfillment Hub, San Francisco, CA",
                baseTime
        ));

        if (!"ORDER_PLACED".equals(r.getStatus())) {
            checkpoints.add(new TrackingCheckpoint(
                    "LABEL_CREATED",
                    "Electronic shipping info received and label printed",
                    "San Francisco Logistics Facility",
                    baseTime.plus(30, ChronoUnit.MINUTES)
            ));
        }

        if ("IN_TRANSIT".equals(r.getStatus()) || "OUT_FOR_DELIVERY".equals(r.getStatus()) || "DELIVERED".equals(r.getStatus())) {
            checkpoints.add(new TrackingCheckpoint(
                    "IN_TRANSIT",
                    "Package departed facility and in transit to local distribution",
                    "Regional Sort Hub, Oakland, CA",
                    baseTime.plus(4, ChronoUnit.HOURS)
            ));
        }

        if ("OUT_FOR_DELIVERY".equals(r.getStatus()) || "DELIVERED".equals(r.getStatus())) {
            checkpoints.add(new TrackingCheckpoint(
                    "OUT_FOR_DELIVERY",
                    "Package loaded on delivery truck, out for delivery",
                    r.getCity() + ", " + r.getState(),
                    baseTime.plus(24, ChronoUnit.HOURS)
            ));
        }

        if ("DELIVERED".equals(r.getStatus())) {
            checkpoints.add(new TrackingCheckpoint(
                    "DELIVERED",
                    "Package delivered. Left at front porch/door.",
                    r.getCity() + ", " + r.getState(),
                    baseTime.plus(28, ChronoUnit.HOURS)
            ));
        }

        String destination = String.format("%s, %s %s, %s",
                r.getCity() != null ? r.getCity() : "",
                r.getState() != null ? r.getState() : "",
                r.getPostalCode() != null ? r.getPostalCode() : "",
                r.getCountry() != null ? r.getCountry() : "");

        return new TrackingResponse(
                r.getTrackingNumber(),
                r.getOrderId(),
                r.getPaymentId(),
                r.getCarrier(),
                r.getShippingMethod(),
                r.getStatus(),
                r.getRecipientName(),
                destination,
                r.getLabelUrl(),
                r.getEstimatedDelivery(),
                r.getCreatedAt(),
                checkpoints
        );
    }
}
