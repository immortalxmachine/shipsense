package com.shipsense.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Feature vector sent to the Python ML service {@code POST /predict}.
 * JSON uses snake_case to match the FastAPI/Pydantic model on the other side.
 */
public record MlPredictRequest(
        @JsonProperty("distance_km") double distanceKm,
        @JsonProperty("weight_kg") double weightKg,
        @JsonProperty("service_type") String serviceType,
        @JsonProperty("day_of_week") int dayOfWeek,      // 0=Mon .. 6=Sun (matches pandas)
        @JsonProperty("month") int month,                // 1..12
        @JsonProperty("is_festival_season") boolean festivalSeason,
        @JsonProperty("is_monsoon") boolean monsoon,
        @JsonProperty("hub_load") int hubLoad
) {
}
