package com.shipsense.dto;

import com.shipsense.entity.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Payload to append a tracking event to a shipment.
 *
 * @param hubCode   hub where the scan happened
 * @param eventType the scan type
 * @param eventTime optional; defaults to "now" when null
 */
public record TrackingEventRequest(
        @NotBlank @Size(min = 3, max = 3) String hubCode,
        @NotNull EventType eventType,
        OffsetDateTime eventTime
) {
}
