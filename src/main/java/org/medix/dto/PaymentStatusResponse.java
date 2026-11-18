package org.medix.dto;

public record PaymentStatusResponse(
        String paymentId,
        String sessionId,
        String paymentIntentId,
        String orderId,
        String status,
        Long amount,
        String currency,
        String customerEmail,
        String paymentMethod,
        String updatedAt
) {
}
