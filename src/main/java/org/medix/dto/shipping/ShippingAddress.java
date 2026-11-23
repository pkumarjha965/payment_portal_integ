package org.medix.dto.shipping;

public record ShippingAddress(
        String recipientName,
        String street,
        String city,
        String state,
        String postalCode,
        String country
) {
}
