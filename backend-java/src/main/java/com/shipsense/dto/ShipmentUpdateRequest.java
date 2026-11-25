package com.shipsense.dto;

import com.shipsense.entity.ServiceType;
import com.shipsense.entity.ShipmentStatus;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Payload to update a shipment (PUT). All fields are optional — only the
 * non-null ones are applied. Setting status to DELIVERED without a deliveredAt
 * defaults the delivery time to "now" in the service.
 */
public record ShipmentUpdateRequest(
        @DecimalMin(value = "0.01", message = "weightKg must be positive")
        BigDecimal weightKg,
        ServiceType serviceType,
        ShipmentStatus status,
        OffsetDateTime promisedAt,
        OffsetDateTime deliveredAt
) {
}
