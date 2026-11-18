package org.medix.dto;

public record PaymentValidationResponse(
        String paymentId,
        String status,
        boolean isValid,
        String message,
        Long amount,
        String currency,
        String customerEmail
) {
}
