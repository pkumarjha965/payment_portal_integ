package org.medix.dto;

public record CreatePaymentOrderResponse(
        String keyId,
        String orderId,
        Long amount,
        String currency,
        String status
) {
}