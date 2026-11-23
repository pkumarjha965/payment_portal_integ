package org.medix.dto.shipping;

import java.util.List;

public record ShippingRateResponse(
        List<ShippingRateOption> options
) {
}
