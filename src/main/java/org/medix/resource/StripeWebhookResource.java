package org.medix.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.medix.service.PaymentService;

import java.util.Map;

@Path("/api/payments/webhook")
public class StripeWebhookResource {

    private static final Logger LOG = Logger.getLogger(StripeWebhookResource.class);

    @Inject
    PaymentService paymentService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response handleWebhook(
            String payload,
            @HeaderParam("Stripe-Signature") String signature) {

        try {
            paymentService.processWebhook(payload, signature);
            return Response.ok(Map.of("received", true)).build();
        } catch (Exception e) {
            LOG.errorf("Error processing Stripe webhook: %s", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage() != null ? e.getMessage() : "Webhook verification failed"))
                    .build();
        }
    }
}
