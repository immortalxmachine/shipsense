package com.shipsense.service;

import com.shipsense.dto.RouteResponse;
import com.shipsense.entity.Hub;
import com.shipsense.entity.Route;
import com.shipsense.exception.NotFoundException;
import com.shipsense.repository.HubRepository;
import com.shipsense.repository.RouteRepository;
import com.shipsense.routing.DijkstraRouteOptimizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Tests {@link RouteService} wired to the REAL {@link DijkstraRouteOptimizer}
 * (only the repositories are mocked), so this exercises graph loading + the
 * algorithm together.
 */
@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock RouteRepository routeRepository;
    @Mock HubRepository hubRepository;

    RouteService routeService;

    private static Hub hub(String code) {
        return new Hub(code, code + " Hub", code, "SE", new BigDecimal("0.0"), new BigDecimal("0.0"));
    }

    private static Route route(Hub a, Hub b, double km, double hrs) {
        return new Route(a, b, new BigDecimal(String.valueOf(km)), new BigDecimal(String.valueOf(hrs)));
    }

    @BeforeEach
    void setUp() {
        routeService = new RouteService(routeRepository, hubRepository, new DijkstraRouteOptimizer());

        Hub sp = hub("SP");
        Hub mg = hub("MG");
        Hub rs = hub("RS");
        // SP-MG-RS = 350 + 2000 = 2350  beats direct SP-RS = 2500
        List<Route> edges = List.of(
                route(sp, mg, 350, 6), route(mg, sp, 350, 6),
                route(mg, rs, 2000, 33), route(rs, mg, 2000, 33),
                route(sp, rs, 2500, 42), route(rs, sp, 2500, 42)
        );
        lenient().when(routeRepository.findAll()).thenReturn(edges);
    }

    @Test
    void findOptimal_choosesShortestAndNormalisesCase() {
        when(hubRepository.existsByCode("SP")).thenReturn(true);
        when(hubRepository.existsByCode("RS")).thenReturn(true);

        RouteResponse resp = routeService.findOptimal("sp", "rs"); // lower-case input

        assertThat(resp.from()).isEqualTo("SP");
        assertThat(resp.to()).isEqualTo("RS");
        assertThat(resp.path()).containsExactly("SP", "MG", "RS");
        assertThat(resp.totalDistanceKm()).isEqualTo(2350.0);
    }

    @Test
    void findOptimal_throwsWhenHubUnknown() {
        when(hubRepository.existsByCode("SP")).thenReturn(true);
        when(hubRepository.existsByCode("ZZ")).thenReturn(false);

        assertThatThrownBy(() -> routeService.findOptimal("SP", "ZZ"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("ZZ");
    }
}
