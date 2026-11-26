package com.shipsense.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Payload to create a shipment. Bean-validation annotations are enforced by
 * {@code @Valid} in the controller; failures become a 400 via the global handler.
 *
 * @param trackingNo     optional; auto-generated when null/blank
 * @param originHubCode  2-letter state (UF) code, e.g. "SP" (seller state)
 * @param destHubCode    2-letter state (UF) code, e.g. "RJ" (customer state)
 * @param weightKg       parcel weight, must be positive
 * @param freightValue   freight paid in BRL, must be non-negative
 * @param promisedAt     estimated delivery date
 */
public record ShipmentRequest(
        @Size(max = 20) String trackingNo,

        @NotBlank @Size(min = 2, max = 2) String originHubCode,

        @NotBlank @Size(min = 2, max = 2) String destHubCode,

        @NotNull @DecimalMin(value = "0.01", message = "weightKg must be positive")
        BigDecimal weightKg,

        @NotNull @DecimalMin(value = "0.0", message = "freightValue must be non-negative")
        BigDecimal freightValue,

        @NotNull OffsetDateTime promisedAt
) {
}
