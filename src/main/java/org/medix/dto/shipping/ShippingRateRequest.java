package org.medix.dto.shipping;

public record ShippingRateRequest(
        String destinationPostalCode,
        String country,
        Long subtotal
) {
}
