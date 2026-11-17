package org.medix.resource;

import com.stripe.StripeClient;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.inject.Inject;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;

@Path("/api/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    @Inject
    @ConfigProperty(name="stripe.secret-key")
    String apiKey;

    @POST
    @Path("/checkout")
    public Map<String, String> createCheckout() throws Exception {

        StripeClient client = new StripeClient(apiKey);


        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)

                        .setSuccessUrl(
                                "http://localhost:5173/payment/loading" +
                                        "?session_id={CHECKOUT_SESSION_ID}"
                                // Order - page ; Loader
                                // polling
                                // Redirect order confirmation page
                        )

                        .setCancelUrl(
                                "http://localhost:5173/payment/cancel"
                        )

                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPriceData(
                                                SessionCreateParams.LineItem.PriceData.builder()
                                                        .setCurrency("usd")
                                                        .setUnitAmount(10000L)
                                                        .setProductData(
                                                                SessionCreateParams.LineItem.PriceData.ProductData
                                                                        .builder()
                                                                        .setName("POC Product")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .build();

        Session session = client.v1().checkout().sessions().create(params);

        return Map.of(
                "sessionId", session.getId(),
                "checkoutUrl", session.getUrl()
        );
    }
}