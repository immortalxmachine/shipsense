package com.shipsense.dto;

import com.shipsense.entity.EventType;
import com.shipsense.entity.TrackingEvent;

import java.time.OffsetDateTime;

/** API view of a {@link TrackingEvent}. */
public record TrackingEventResponse(
        Integer id,
        String hubCode,
        String hubCity,
        EventType eventType,
        OffsetDateTime eventTime
) {

    public static TrackingEventResponse from(TrackingEvent e) {
        return new TrackingEventResponse(
                e.getId(),
                e.getHub().getCode(),
                e.getHub().getCity(),
                e.getEventType(),
                e.getEventTime()
        );
    }
}
