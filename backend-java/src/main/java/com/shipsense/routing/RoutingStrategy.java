package com.shipsense.routing;

import java.util.List;

/**
 * Strategy for finding an optimal path between two hubs.
 *
 * <p>This interface is the <b>Strategy</b> pattern: the route optimization
 * algorithm is pluggable. {@link DijkstraRouteOptimizer} is the default
 * implementation; an A* or contraction-hierarchies variant could be dropped
 * in without touching callers.
 */
public interface RoutingStrategy {

    /**
     * @param fromCode origin hub code
     * @param toCode   destination hub code
     * @param edges    all directed edges in the network
     * @return the optimal route (shortest by distance)
     */
    RouteResult findOptimalRoute(String fromCode, String toCode, List<RouteEdge> edges);
}
