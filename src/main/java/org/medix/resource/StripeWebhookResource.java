package org.medix.resource;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/payments/webhook")
public class StripeWebhookResource {

    @ConfigProperty(name = "stripe.webhook-secret")
    String webhookSecret;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response handleWebhook(
            String payload,
            @HeaderParam("Stripe-Signature") String signature) {

        try {

            Event event = Webhook.constructEvent(
                    payload,
                    signature,
                    webhookSecret
            );

            switch (event.getType()) {

                case "checkout.session.completed":

                    Session session = (Session) event
                            .getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow();

                    System.out.println(
                            "Payment completed: " + session.getId()
                    );

                    System.out.println(
                            "Payment details: " + session.toJson()
                    );

                    // TODO:
                    // Retrieve internal order reference
                    // Update payment status to PAID

                    break;

                case "checkout.session.expired":

                    System.out.println("Checkout session expired");

                    break;

                default:

                    System.out.println(
                            "Unhandled event: " + event.getType()
                    );
            }

            return Response.ok().build();

        } catch (Exception e) {

            return Response.status(400).build();

        }
    }
}