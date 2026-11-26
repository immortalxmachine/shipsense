package com.shipsense.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Response body from the Python ML service {@code POST /predict}. */
public record MlPredictResponse(
        @JsonProperty("predicted_delivery_days") Double predictedDeliveryDays,
        @JsonProperty("on_time_probability") Double onTimeProbability
) {
}
