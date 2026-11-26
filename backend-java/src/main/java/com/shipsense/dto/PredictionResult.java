package com.shipsense.dto;

/**
 * Delivery-time prediction returned by the Python ML service (or a fallback
 * when it is unavailable).
 *
 * @param predictedDeliveryDays estimated delivery time in days
 * @param onTimeProbability     probability the parcel arrives on time, 0..1 (may be null)
 * @param modelAvailable        false when the ML service was down and a fallback was used
 */
public record PredictionResult(Double predictedDeliveryDays,
                               Double onTimeProbability,
                               boolean modelAvailable) {

    /** Neutral fallback used when the ML service cannot be reached. */
    public static PredictionResult unavailable() {
        return new PredictionResult(null, null, false);
    }
}
