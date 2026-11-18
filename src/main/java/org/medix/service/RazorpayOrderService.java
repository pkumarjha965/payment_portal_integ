package org.medix.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import org.medix.dto.CreatePaymentOrderResponse;

@ApplicationScoped
public class RazorpayOrderService {

    public CreatePaymentOrderResponse createOrder(String invoiceId) {

        if (!"INV-1001".equals(invoiceId)) {
            throw new WebApplicationException(
                    "Invoice not found: " + invoiceId, 404);
        }

        // Simulate Razorpay order creation
        return new CreatePaymentOrderResponse(
                "rzp_test_dummy123",
                "order_dummy_123456",
                10000L,
                "INR",
                "created"
        );
    }
}