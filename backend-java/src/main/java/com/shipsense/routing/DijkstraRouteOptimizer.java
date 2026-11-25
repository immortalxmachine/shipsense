package com.shipsense.routing;

import com.shipsense.exception.NoRouteFoundException;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Shortest-path route optimizer using <b>Dijkstra's algorithm</b>.
 *
 * <p>The graph is an adjacency list built from the {@code routes} table. Edge
 * weight is road distance in kilometres; we also accumulate travel hours along
 * the chosen path to produce an ETA.
 *
 * <p><b>Why Dijkstra:</b> all edge weights are non-negative (distances), so
 * Dijkstra is correct and efficient. A {@link PriorityQueue} (binary min-heap)
 * always expands the closest unsettled node first.
 *
 * <p><b>Time complexity:</b> {@code O((V + E) log V)} — each vertex is polled
 * once ({@code V} heap removals) and each edge may push a new, smaller-distance
 * entry ({@code E} heap insertions); every heap operation costs {@code log V}.
 * <br><b>Space complexity:</b> {@code O(V + E)} for the adjacency list and the
 * distance / predecessor maps.
 */
@Component
public class DijkstraRouteOptimizer implements RoutingStrategy {

    /** A node waiting in the priority queue, ordered by cumulative distance. */
    private record QueueEntry(String hub, double distance) {
    }

    @Override
    public RouteResult findOptimalRoute(String fromCode, String toCode, List<RouteEdge> edges) {
        Objects.requireNonNull(fromCode, "fromCode");
        Objects.requireNonNull(toCode, "toCode");

        // Trivial case: origin equals destination -> zero-length path.
        if (fromCode.equals(toCode)) {
            return new RouteResult(List.of(fromCode), 0.0, 0.0);
        }

        // 1. Build the adjacency list: hub code -> outgoing edges.
        Map<String, List<RouteEdge>> adjacency = new HashMap<>();
        for (RouteEdge e : edges) {
            adjacency.computeIfAbsent(e.from(), k -> new ArrayList<>()).add(e);
        }

        // 2. Dijkstra state.
        Map<String, Double> dist = new HashMap<>();      // best known distance to each hub
        Map<String, RouteEdge> prevEdge = new HashMap<>(); // edge used to reach each hub (for path + hours)
        Set<String> settled = new HashSet<>();            // hubs whose shortest distance is final

        dist.put(fromCode, 0.0);
        PriorityQueue<QueueEntry> pq =
                new PriorityQueue<>(Comparator.comparingDouble(QueueEntry::distance));
        pq.add(new QueueEntry(fromCode, 0.0));

        // 3. Main loop: always expand the closest unsettled hub.
        while (!pq.isEmpty()) {
            QueueEntry current = pq.poll();
            String u = current.hub();

            // Skip stale queue entries (a shorter distance was found after this was queued).
            if (!settled.add(u)) {
                continue;
            }
            if (u.equals(toCode)) {
                break; // destination settled — we have the shortest path
            }

            for (RouteEdge edge : adjacency.getOrDefault(u, List.of())) {
                String v = edge.to();
                if (settled.contains(v)) {
                    continue;
                }
                double candidate = dist.get(u) + edge.distanceKm();
                if (candidate < dist.getOrDefault(v, Double.POSITIVE_INFINITY)) {
                    dist.put(v, candidate);
                    prevEdge.put(v, edge);
                    pq.add(new QueueEntry(v, candidate));
                }
            }
        }

        // 4. No path reached the destination.
        if (!dist.containsKey(toCode)) {
            throw new NoRouteFoundException(fromCode, toCode);
        }

        // 5. Reconstruct the path (walk predecessors backwards) and sum hours.
        LinkedList<String> path = new LinkedList<>();
        double totalHours = 0.0;
        String node = toCode;
        path.addFirst(node);
        while (!node.equals(fromCode)) {
            RouteEdge edge = prevEdge.get(node);
            totalHours += edge.travelHours();
            node = edge.from();
            path.addFirst(node);
        }

        return new RouteResult(path, dist.get(toCode), totalHours);
    }
}
