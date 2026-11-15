package org.medix.dto;

public record CreatePaymentOrderRequest(
        String invoiceId
) {
}