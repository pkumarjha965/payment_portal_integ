package org.medix.dto.shipping;

import java.time.Instant;

public record TrackingCheckpoint(
        String status,
        String description,
        String location,
        Instant timestamp
) {
}
