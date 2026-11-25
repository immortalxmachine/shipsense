package com.shipsense.repository;

import com.shipsense.entity.Route;
import com.shipsense.entity.RouteId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link Route}. {@code findAll()} (inherited) returns every
 * directed edge; the {@code RouteOptimizer} builds an adjacency list from it.
 */
public interface RouteRepository extends JpaRepository<Route, RouteId> {
}
