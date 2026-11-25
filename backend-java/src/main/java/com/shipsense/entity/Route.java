package com.shipsense.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * A directed road link between two hubs — an edge in the route graph.
 * Maps to the {@code routes} table, whose primary key is {@code (from_hub, to_hub)}.
 *
 * <p>The composite key is declared with {@link RouteId} via {@code @IdClass}.
 */
@Entity
@Table(name = "routes")
@IdClass(RouteId.class)
public class Route {

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_hub")
    private Hub fromHub;

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_hub")
    private Hub toHub;

    @Column(name = "distance_km", nullable = false, precision = 7, scale = 1)
    private BigDecimal distanceKm;

    @Column(name = "avg_travel_hours", nullable = false, precision = 5, scale = 1)
    private BigDecimal avgTravelHours;

    protected Route() {
    }

    public Route(Hub fromHub, Hub toHub, BigDecimal distanceKm, BigDecimal avgTravelHours) {
        this.fromHub = fromHub;
        this.toHub = toHub;
        this.distanceKm = distanceKm;
        this.avgTravelHours = avgTravelHours;
    }

    public Hub getFromHub()            { return fromHub; }
    public Hub getToHub()              { return toHub; }
    public BigDecimal getDistanceKm()  { return distanceKm; }
    public BigDecimal getAvgTravelHours() { return avgTravelHours; }
}
