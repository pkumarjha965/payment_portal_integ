package org.medix.dto.shipping;

public record CreateShipmentRequest(
        String orderId,
        String paymentId,
        String shippingMethod,
        ShippingAddress address,
        String itemsDescription
) {
}
