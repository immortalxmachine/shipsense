package com.shipsense.controller;

import com.shipsense.dto.RouteResponse;
import com.shipsense.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
@Tag(name = "Routes", description = "Shortest-path route optimization")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/optimal")
    @Operation(summary = "Find the optimal (shortest-distance) route between two hubs")
    public RouteResponse optimal(
            @Parameter(example = "CHN") @RequestParam String from,
            @Parameter(example = "DEL") @RequestParam String to) {
        return routeService.findOptimal(from, to);
    }
}
