package com.shipsense.controller;

import com.shipsense.dto.*;
import com.shipsense.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@Tag(name = "Shipments", description = "Create, track and manage parcels")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    @Operation(summary = "Create a shipment")
    public ResponseEntity<ShipmentResponse> create(@Valid @RequestBody ShipmentRequest request,
                                                    UriComponentsBuilder uriBuilder) {
        ShipmentResponse created = shipmentService.create(request);
        URI location = uriBuilder.path("/api/shipments/{trackingNo}")
                .buildAndExpand(created.trackingNo()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List all shipments")
    public List<ShipmentResponse> list() {
        return shipmentService.findAll();
    }

    @GetMapping("/{trackingNo}")
    @Operation(summary = "Track a shipment by tracking number (includes ML delay prediction)")
    public ShipmentResponse track(@PathVariable String trackingNo) {
        return shipmentService.getByTrackingNo(trackingNo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a shipment by id")
    public ShipmentResponse update(@PathVariable Integer id,
                                   @Valid @RequestBody ShipmentUpdateRequest request) {
        return shipmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a shipment by id")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        shipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{trackingNo}/events")
    @Operation(summary = "List the tracking-event timeline for a shipment")
    public List<TrackingEventResponse> events(@PathVariable String trackingNo) {
        return shipmentService.getEvents(trackingNo);
    }

    @PostMapping("/{id}/events")
    @Operation(summary = "Append a tracking event to a shipment by id")
    public ResponseEntity<TrackingEventResponse> addEvent(@PathVariable Integer id,
                                                          @Valid @RequestBody TrackingEventRequest request) {
        TrackingEventResponse created = shipmentService.addEvent(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
