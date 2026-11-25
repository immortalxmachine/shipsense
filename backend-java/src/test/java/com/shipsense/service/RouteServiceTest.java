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
        return new Hub(code, code + " Hub", code, new BigDecimal("0.0"), new BigDecimal("0.0"));
    }

    private static Route route(Hub a, Hub b, double km, double hrs) {
        return new Route(a, b, new BigDecimal(String.valueOf(km)), new BigDecimal(String.valueOf(hrs)));
    }

    @BeforeEach
    void setUp() {
        routeService = new RouteService(routeRepository, hubRepository, new DijkstraRouteOptimizer());

        Hub chn = hub("CHN");
        Hub blr = hub("BLR");
        Hub del = hub("DEL");
        // CHN-BLR-DEL = 350 + 2000 = 2350  beats direct CHN-DEL = 2500
        List<Route> edges = List.of(
                route(chn, blr, 350, 6), route(blr, chn, 350, 6),
                route(blr, del, 2000, 33), route(del, blr, 2000, 33),
                route(chn, del, 2500, 42), route(del, chn, 2500, 42)
        );
        lenient().when(routeRepository.findAll()).thenReturn(edges);
    }

    @Test
    void findOptimal_choosesShortestAndNormalisesCase() {
        when(hubRepository.existsByCode("CHN")).thenReturn(true);
        when(hubRepository.existsByCode("DEL")).thenReturn(true);

        RouteResponse resp = routeService.findOptimal("chn", "del"); // lower-case input

        assertThat(resp.from()).isEqualTo("CHN");
        assertThat(resp.to()).isEqualTo("DEL");
        assertThat(resp.path()).containsExactly("CHN", "BLR", "DEL");
        assertThat(resp.totalDistanceKm()).isEqualTo(2350.0);
    }

    @Test
    void findOptimal_throwsWhenHubUnknown() {
        when(hubRepository.existsByCode("CHN")).thenReturn(true);
        when(hubRepository.existsByCode("XXX")).thenReturn(false);

        assertThatThrownBy(() -> routeService.findOptimal("CHN", "XXX"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("XXX");
    }
}
