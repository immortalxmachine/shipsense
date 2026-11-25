package com.shipsense.routing;

import java.util.List;

/**
 * The outcome of a shortest-path search.
 *
 * @param path            ordered list of hub codes from origin to destination
 * @param totalDistanceKm sum of edge distances along the path
 * @param totalHours      sum of edge travel times along the path
 */
public record RouteResult(List<String> path, double totalDistanceKm, double totalHours) {
}
