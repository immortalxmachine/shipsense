package com.shipsense.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Feature vector sent to the Python ML service {@code POST /predict}.
 * JSON uses snake_case to match the FastAPI/Pydantic model on the other side.
 * Every field is derived from real shipment attributes (no synthetic inputs).
 */
public record MlPredictRequest(
        @JsonProperty("distance_km") double distanceKm,
        @JsonProperty("weight_kg") double weightKg,
        @JsonProperty("freight_value") double freightValue,
        @JsonProperty("same_state") boolean sameState,
        @JsonProperty("purchase_dow") int purchaseDow,     // 0=Mon .. 6=Sun (matches pandas)
        @JsonProperty("purchase_month") int purchaseMonth, // 1..12
        @JsonProperty("cust_region") String custRegion,    // N | NE | CO | SE | S
        @JsonProperty("seller_region") String sellerRegion,
        @JsonProperty("estimated_days") Double estimatedDays // for on-time prob; may be null
) {
}
