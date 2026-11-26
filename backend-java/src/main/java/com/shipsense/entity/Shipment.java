package com.shipsense.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * A parcel moving from an origin hub to a destination hub.
 * Maps to the {@code shipments} table.
 */
@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tracking_no", nullable = false, unique = true, length = 20)
    private String trackingNo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "origin_hub")
    private Hub originHub;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "dest_hub")
    private Hub destHub;

    @Column(name = "weight_kg", nullable = false, precision = 7, scale = 2)
    private BigDecimal weightKg;

    /** Total freight paid for the shipment, in BRL (a real Olist order attribute). */
    @Column(name = "freight_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal freightValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status = ShipmentStatus.CREATED;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "promised_at", nullable = false)
    private OffsetDateTime promisedAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    protected Shipment() {
    }

    public Shipment(String trackingNo, Hub originHub, Hub destHub, BigDecimal weightKg,
                    BigDecimal freightValue, OffsetDateTime promisedAt) {
        this.trackingNo = trackingNo;
        this.originHub = originHub;
        this.destHub = destHub;
        this.weightKg = weightKg;
        this.freightValue = freightValue;
        this.promisedAt = promisedAt;
    }

    public Integer getId()                 { return id; }
    public String getTrackingNo()          { return trackingNo; }
    public Hub getOriginHub()              { return originHub; }
    public Hub getDestHub()                { return destHub; }
    public BigDecimal getWeightKg()        { return weightKg; }
    public BigDecimal getFreightValue()    { return freightValue; }
    public ShipmentStatus getStatus()      { return status; }
    public OffsetDateTime getCreatedAt()   { return createdAt; }
    public OffsetDateTime getPromisedAt()  { return promisedAt; }
    public OffsetDateTime getDeliveredAt() { return deliveredAt; }

    public void setTrackingNo(String trackingNo)       { this.trackingNo = trackingNo; }
    public void setOriginHub(Hub originHub)            { this.originHub = originHub; }
    public void setDestHub(Hub destHub)                { this.destHub = destHub; }
    public void setWeightKg(BigDecimal weightKg)           { this.weightKg = weightKg; }
    public void setFreightValue(BigDecimal freightValue)   { this.freightValue = freightValue; }
    public void setStatus(ShipmentStatus status)       { this.status = status; }
    public void setPromisedAt(OffsetDateTime promisedAt)   { this.promisedAt = promisedAt; }
    public void setDeliveredAt(OffsetDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
}
