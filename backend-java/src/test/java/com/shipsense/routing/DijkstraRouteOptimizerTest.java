package com.shipsense.routing;

import com.shipsense.exception.NoRouteFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for {@link DijkstraRouteOptimizer} — no Spring, no DB.
 *
 * <p>Graph used (undirected; each edge added both ways):
 * <pre>
 *        A --4-- B
 *        |      /|
 *        1    2  5
 *        |  /    |
 *        C --8-- D --3-- E        F (isolated)
 * </pre>
 */
class DijkstraRouteOptimizerTest {

    private final DijkstraRouteOptimizer optimizer = new DijkstraRouteOptimizer();

    /** Build the sample graph. travelHours chosen so path A-C-B-D-E sums to 5.0h. */
    private List<RouteEdge> sampleGraph() {
        List<RouteEdge> edges = new ArrayList<>();
        addBoth(edges, "A", "B", 4, 2.0);
        addBoth(edges, "A", "C", 1, 0.5);
        addBoth(edges, "C", "B", 2, 1.0);
        addBoth(edges, "B", "D", 5, 2.0);
        addBoth(edges, "C", "D", 8, 4.0);
        addBoth(edges, "D", "E", 3, 1.5);
        // "F" has no edges -> unreachable.
        return edges;
    }

    private void addBoth(List<RouteEdge> edges, String a, String b, double km, double hrs) {
        edges.add(new RouteEdge(a, b, km, hrs));
        edges.add(new RouteEdge(b, a, km, hrs));
    }

    @Test
    @DisplayName("finds the shortest path and sums distance + hours along it")
    void findsShortestPath() {
        RouteResult result = optimizer.findOptimalRoute("A", "E", sampleGraph());

        // A-C(1)-B(2)-D(5)-E(3) = 11 km  beats  A-B-D-E (12) and A-C-D-E (12)
        assertThat(result.path()).containsExactly("A", "C", "B", "D", "E");
        assertThat(result.totalDistanceKm()).isEqualTo(11.0);
        assertThat(result.totalHours()).isEqualTo(5.0); // 0.5 + 1.0 + 2.0 + 1.5
    }

    @Test
    @DisplayName("prefers a shorter two-hop path over a longer direct edge")
    void prefersShorterIndirectPath() {
        RouteResult result = optimizer.findOptimalRoute("A", "B", sampleGraph());

        // Direct A-B is 4, but A-C-B is 1+2 = 3.
        assertThat(result.path()).containsExactly("A", "C", "B");
        assertThat(result.totalDistanceKm()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("same origin and destination returns a zero-length route")
    void sameHubIsTrivial() {
        RouteResult result = optimizer.findOptimalRoute("A", "A", sampleGraph());

        assertThat(result.path()).containsExactly("A");
        assertThat(result.totalDistanceKm()).isZero();
        assertThat(result.totalHours()).isZero();
    }

    @Test
    @DisplayName("throws when the destination is unreachable")
    void noPathThrows() {
        assertThatThrownBy(() -> optimizer.findOptimalRoute("A", "F", sampleGraph()))
                .isInstanceOf(NoRouteFoundException.class)
                .hasMessageContaining("A")
                .hasMessageContaining("F");
    }

    @Test
    @DisplayName("throws when the origin hub has no outgoing edges")
    void unknownOriginThrows() {
        assertThatThrownBy(() -> optimizer.findOptimalRoute("Z", "E", sampleGraph()))
                .isInstanceOf(NoRouteFoundException.class);
    }
}
