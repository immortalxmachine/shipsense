package com.shipsense.routing;

/**
 * A directed edge in the route graph, keyed by hub codes (e.g. "CHN" -> "BLR").
 * Immutable value object the {@link RoutingStrategy} consumes — deliberately
 * decoupled from JPA entities so the algorithm is trivial to unit-test.
 *
 * @param from         origin hub code
 * @param to           destination hub code
 * @param distanceKm   road distance in kilometres (used as the edge weight)
 * @param travelHours  average drive time in hours (summed for the ETA)
 */
public record RouteEdge(String from, String to, double distanceKm, double travelHours) {
}
