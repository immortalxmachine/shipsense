package com.shipsense.dto;

import com.shipsense.routing.RouteResult;

import java.util.List;

/**
 * API view of an optimal route.
 *
 * @param from            origin hub code
 * @param to              destination hub code
 * @param path            ordered hub codes along the route
 * @param totalDistanceKm total road distance
 * @param estimatedHours  summed travel time (ETA) in hours
 */
public record RouteResponse(
        String from,
        String to,
        List<String> path,
        double totalDistanceKm,
        double estimatedHours
) {

    public static RouteResponse from(String fromCode, String toCode, RouteResult r) {
        return new RouteResponse(fromCode, toCode, r.path(),
                r.totalDistanceKm(), r.totalHours());
    }
}
