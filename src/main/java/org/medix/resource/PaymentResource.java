package org.medix.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.medix.dto.CheckoutSessionRequest;
import org.medix.dto.PaymentValidationResponse;
import org.medix.service.PaymentService;

import java.util.Map;

@Path("/api/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    @Inject
    PaymentService paymentService;

    @POST
    @Path("/checkout")
    public Response createCheckout(CheckoutSessionRequest request) throws Exception {
        Map<String, Object> checkout = paymentService.createCheckout(request);
        return Response.ok(checkout).build();
    }

    /**
     * Reads the database for a given paymentId (sessionId or paymentIntentId)
     * and returns the status to be consumed by the UI service.
     */
    @GET
    @Path("/status/{paymentId}")
    public Response getPaymentStatus(@PathParam("paymentId") String paymentId) {
        return paymentService.getPaymentStatus(paymentId)
                .map(status -> Response.ok(status).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Payment not found for ID: " + paymentId))
                        .build());
    }

    /**
     * Alias for UI convenience: /api/payments/{paymentId}
     */
    @GET
    @Path("/{paymentId}")
    public Response getPaymentById(@PathParam("paymentId") String paymentId) {
        return getPaymentStatus(paymentId);
    }

    /**
     * Payment validation API: validates status in database, and verifies with Stripe
     * if webhook event has not yet been processed.
     */
    @GET
    @Path("/validate/{paymentId}")
    public Response validatePaymentGet(@PathParam("paymentId") String paymentId) {
        PaymentValidationResponse response = paymentService.validatePayment(paymentId);
        return Response.ok(response).build();
    }

    @POST
    @Path("/validate/{paymentId}")
    public Response validatePaymentPost(@PathParam("paymentId") String paymentId) {
        PaymentValidationResponse response = paymentService.validatePayment(paymentId);
        return Response.ok(response).build();
    }
}
