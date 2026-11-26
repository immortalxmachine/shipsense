package com.shipsense.dto;

import com.shipsense.entity.Shipment;
import com.shipsense.entity.ShipmentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * API view of a {@link Shipment}, optionally enriched with an ML delivery-time
 * prediction.
 */
public record ShipmentResponse(
        Integer id,
        String trackingNo,
        String originCode,
        String originCity,
        String destCode,
        String destCity,
        BigDecimal weightKg,
        BigDecimal freightValue,
        ShipmentStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime promisedAt,
        OffsetDateTime deliveredAt,
        Double predictedDeliveryDays,
        Double onTimeProbability
) {

    /** Build a response without a prediction. */
    public static ShipmentResponse from(Shipment s) {
        return from(s, null);
    }

    /** Build a response, attaching the prediction when one is available. */
    public static ShipmentResponse from(Shipment s, PredictionResult prediction) {
        Double days = (prediction != null) ? prediction.predictedDeliveryDays() : null;
        Double prob = (prediction != null) ? prediction.onTimeProbability() : null;
        return new ShipmentResponse(
                s.getId(),
                s.getTrackingNo(),
                s.getOriginHub().getCode(),
                s.getOriginHub().getCity(),
                s.getDestHub().getCode(),
                s.getDestHub().getCity(),
                s.getWeightKg(),
                s.getFreightValue(),
                s.getStatus(),
                s.getCreatedAt(),
                s.getPromisedAt(),
                s.getDeliveredAt(),
                days,
                prob
        );
    }
}
