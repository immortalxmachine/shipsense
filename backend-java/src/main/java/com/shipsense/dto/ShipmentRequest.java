package com.shipsense.dto;

import com.shipsense.entity.ServiceType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Payload to create a shipment. Bean-validation annotations are enforced by
 * {@code @Valid} in the controller; failures become a 400 via the global handler.
 *
 * @param trackingNo     optional; auto-generated when null/blank
 * @param originHubCode  3-letter hub code, e.g. "DEL"
 * @param destHubCode    3-letter hub code, e.g. "CHN"
 * @param weightKg       parcel weight, must be positive
 * @param serviceType    STANDARD | EXPRESS | PRIORITY
 * @param promisedAt     SLA deadline
 */
public record ShipmentRequest(
        @Size(max = 20) String trackingNo,

        @NotBlank @Size(min = 3, max = 3) String originHubCode,

        @NotBlank @Size(min = 3, max = 3) String destHubCode,

        @NotNull @DecimalMin(value = "0.01", message = "weightKg must be positive")
        BigDecimal weightKg,

        @NotNull ServiceType serviceType,

        @NotNull OffsetDateTime promisedAt
) {
}
