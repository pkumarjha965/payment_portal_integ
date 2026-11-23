package org.medix.dto.shipping;

import java.time.Instant;
import java.util.List;

public record TrackingResponse(
        String trackingNumber,
        String orderId,
        String paymentId,
        String carrier,
        String shippingMethod,
        String status,
        String recipientName,
        String destination,
        String labelUrl,
        Instant estimatedDelivery,
        Instant createdAt,
        List<TrackingCheckpoint> checkpoints
) {
}
