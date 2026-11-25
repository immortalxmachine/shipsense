package com.shipsense.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for {@link Route}.
 *
 * <p>With {@code @IdClass}, the field names here ({@code fromHub}, {@code toHub})
 * must match the {@code @Id} field names on the entity, and their type must match
 * the id type of the referenced {@link Hub} ({@code Integer}). JPA uses this class
 * to represent the two-column primary key {@code (from_hub, to_hub)}.
 */
public class RouteId implements Serializable {

    private Integer fromHub;
    private Integer toHub;

    public RouteId() {
    }

    public RouteId(Integer fromHub, Integer toHub) {
        this.fromHub = fromHub;
        this.toHub = toHub;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteId that)) return false;
        return Objects.equals(fromHub, that.fromHub) && Objects.equals(toHub, that.toHub);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromHub, toHub);
    }
}
