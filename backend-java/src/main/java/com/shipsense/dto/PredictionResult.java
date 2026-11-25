package com.shipsense.dto;

/**
 * Delay prediction returned by the Python ML service (or a fallback when it is
 * unavailable).
 *
 * @param predictedDelayHours estimated delay in hours (0 = expected on time)
 * @param onTimeProbability   probability the parcel arrives on time, 0..1
 * @param modelAvailable      false when the ML service was down and a fallback was used
 */
public record PredictionResult(Double predictedDelayHours,
                               Double onTimeProbability,
                               boolean modelAvailable) {

    /** Neutral fallback used when the ML service cannot be reached. */
    public static PredictionResult unavailable() {
        return new PredictionResult(null, null, false);
    }
}
