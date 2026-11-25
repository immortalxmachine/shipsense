package com.shipsense.controller;

import com.shipsense.dto.HubResponse;
import com.shipsense.service.HubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hubs")
@Tag(name = "Hubs", description = "Sorting facilities in the network")
public class HubController {

    private final HubService hubService;

    public HubController(HubService hubService) {
        this.hubService = hubService;
    }

    @GetMapping
    @Operation(summary = "List all hubs")
    public List<HubResponse> listHubs() {
        return hubService.findAll();
    }
}
