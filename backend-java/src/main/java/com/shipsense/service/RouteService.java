package com.shipsense.service;

import com.shipsense.dto.RouteResponse;
import com.shipsense.entity.Route;
import com.shipsense.exception.NoRouteFoundException;
import com.shipsense.exception.NotFoundException;
import com.shipsense.repository.HubRepository;
import com.shipsense.repository.RouteRepository;
import com.shipsense.routing.RouteEdge;
import com.shipsense.routing.RouteResult;
import com.shipsense.routing.RoutingStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Loads the route graph from the DB and delegates shortest-path computation to
 * a {@link RoutingStrategy} (Dijkstra by default — injected by Spring).
 */
@Service
@Transactional(readOnly = true)
public class RouteService {

    private final RouteRepository routeRepository;
    private final HubRepository hubRepository;
    private final RoutingStrategy routingStrategy;

    public RouteService(RouteRepository routeRepository, HubRepository hubRepository,
                        RoutingStrategy routingStrategy) {
        this.routeRepository = routeRepository;
        this.hubRepository = hubRepository;
        this.routingStrategy = routingStrategy;
    }

    public RouteResponse findOptimal(String fromCode, String toCode) {
        String from = normalize(fromCode);
        String to = normalize(toCode);

        // Fail clearly if either hub code is unknown.
        if (!hubRepository.existsByCode(from)) {
            throw new NotFoundException("Hub not found: " + from);
        }
        if (!hubRepository.existsByCode(to)) {
            throw new NotFoundException("Hub not found: " + to);
        }

        RouteResult result = routingStrategy.findOptimalRoute(from, to, loadEdges());
        return RouteResponse.from(from, to, result);
    }

    /**
     * Shortest road distance between two hubs, used to build the ML feature
     * vector. Returns 0 if no route exists so prediction can still proceed.
     */
    public double shortestDistanceKm(String fromCode, String toCode) {
        try {
            return routingStrategy
                    .findOptimalRoute(normalize(fromCode), normalize(toCode), loadEdges())
                    .totalDistanceKm();
        } catch (NoRouteFoundException ex) {
            return 0.0;
        }
    }

    private List<RouteEdge> loadEdges() {
        return routeRepository.findAll().stream()
                .map(RouteService::toEdge)
                .toList();
    }

    private static RouteEdge toEdge(Route r) {
        return new RouteEdge(
                r.getFromHub().getCode(),
                r.getToHub().getCode(),
                r.getDistanceKm().doubleValue(),
                r.getAvgTravelHours().doubleValue()
        );
    }

    private static String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }
}
