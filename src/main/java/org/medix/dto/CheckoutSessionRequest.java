package org.medix.dto;

public record CheckoutSessionRequest(
        String orderId,
        String productName,
        Long amount,
        String currency,
        String successUrl,
        String cancelUrl
) {
}
