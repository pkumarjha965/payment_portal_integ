package org.medix.dto.shipping;

public record ShippingRateOption(
        String id,
        String name,
        String carrier,
        String estimatedDays,
        Long cost,
        String description
) {
}
