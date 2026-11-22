package org.medix.dto;

import org.medix.dto.shipping.ShippingAddress;

public record CheckoutSessionRequest(
        String orderId,
        String productName,
        Long amount,
        String currency,
        String successUrl,
        String cancelUrl,
        String shippingMethod,
        Long shippingCost,
        ShippingAddress shippingAddress
) {
    public CheckoutSessionRequest(String orderId, String productName, Long amount, String currency, String successUrl, String cancelUrl) {
        this(orderId, productName, amount, currency, successUrl, cancelUrl, "standard", 0L, null);
    }
}
