package com.shipsense.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * A single scan of a shipment at a hub.
 * Maps to the {@code tracking_events} table.
 */
@Entity
@Table(name = "tracking_events")
public class TrackingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "hub_id")
    private Hub hub;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Column(name = "event_time", nullable = false)
    private OffsetDateTime eventTime = OffsetDateTime.now();

    protected TrackingEvent() {
    }

    public TrackingEvent(Shipment shipment, Hub hub, EventType eventType, OffsetDateTime eventTime) {
        this.shipment = shipment;
        this.hub = hub;
        this.eventType = eventType;
        this.eventTime = eventTime;
    }

    public Integer getId()            { return id; }
    public Shipment getShipment()     { return shipment; }
    public Hub getHub()               { return hub; }
    public EventType getEventType()   { return eventType; }
    public OffsetDateTime getEventTime() { return eventTime; }
}
