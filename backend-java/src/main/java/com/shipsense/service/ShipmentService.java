package com.shipsense.service;

import com.shipsense.dto.*;
import com.shipsense.entity.*;
import com.shipsense.exception.BadRequestException;
import com.shipsense.exception.NotFoundException;
import com.shipsense.repository.ShipmentRepository;
import com.shipsense.repository.TrackingEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Core business logic for shipments: CRUD, tracking events, and ML enrichment.
 */
@Service
@Transactional
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final TrackingEventRepository eventRepository;
    private final HubService hubService;
    private final RouteService routeService;
    private final MlPredictionClient mlPredictionClient;

    public ShipmentService(ShipmentRepository shipmentRepository,
                           TrackingEventRepository eventRepository,
                           HubService hubService,
                           RouteService routeService,
                           MlPredictionClient mlPredictionClient) {
        this.shipmentRepository = shipmentRepository;
        this.eventRepository = eventRepository;
        this.hubService = hubService;
        this.routeService = routeService;
        this.mlPredictionClient = mlPredictionClient;
    }

    // ---------------------------------------------------------------- CRUD

    public ShipmentResponse create(ShipmentRequest req) {
        Hub origin = hubService.getByCodeOrThrow(normalize(req.originHubCode()));
        Hub dest = hubService.getByCodeOrThrow(normalize(req.destHubCode()));
        if (origin.getId().equals(dest.getId())) {
            throw new BadRequestException("origin and destination hubs must differ");
        }

        String trackingNo = (req.trackingNo() == null || req.trackingNo().isBlank())
                ? generateTrackingNo()
                : req.trackingNo().trim();
        if (shipmentRepository.existsByTrackingNo(trackingNo)) {
            throw new BadRequestException("trackingNo already exists: " + trackingNo);
        }

        Shipment shipment = new Shipment(trackingNo, origin, dest,
                req.weightKg(), req.serviceType(), req.promisedAt());
        Shipment saved = shipmentRepository.save(shipment);
        return ShipmentResponse.from(saved, predictDelay(saved));
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> findAll() {
        // No per-row prediction here to keep list responses fast.
        return shipmentRepository.findAll().stream()
                .map(ShipmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getByTrackingNo(String trackingNo) {
        Shipment s = requireByTrackingNo(trackingNo);
        return ShipmentResponse.from(s, predictDelay(s));
    }

    public ShipmentResponse update(Integer id, ShipmentUpdateRequest req) {
        Shipment s = requireById(id);
        if (req.weightKg() != null)    s.setWeightKg(req.weightKg());
        if (req.serviceType() != null) s.setServiceType(req.serviceType());
        if (req.promisedAt() != null)  s.setPromisedAt(req.promisedAt());
        if (req.status() != null) {
            s.setStatus(req.status());
            if (req.status() == ShipmentStatus.DELIVERED && s.getDeliveredAt() == null) {
                s.setDeliveredAt(req.deliveredAt() != null ? req.deliveredAt() : OffsetDateTime.now());
            }
        }
        if (req.deliveredAt() != null) s.setDeliveredAt(req.deliveredAt());
        return ShipmentResponse.from(s); // save is implicit (managed entity in a @Transactional method)
    }

    public void delete(Integer id) {
        Shipment s = requireById(id);
        shipmentRepository.delete(s);
    }

    // -------------------------------------------------------- tracking events

    @Transactional(readOnly = true)
    public List<TrackingEventResponse> getEvents(String trackingNo) {
        Shipment s = requireByTrackingNo(trackingNo);
        return eventRepository.findByShipmentIdOrderByEventTimeAsc(s.getId()).stream()
                .map(TrackingEventResponse::from)
                .toList();
    }

    public TrackingEventResponse addEvent(Integer shipmentId, TrackingEventRequest req) {
        Shipment s = requireById(shipmentId);
        Hub hub = hubService.getByCodeOrThrow(normalize(req.hubCode()));
        OffsetDateTime time = req.eventTime() != null ? req.eventTime() : OffsetDateTime.now();

        TrackingEvent event = new TrackingEvent(s, hub, req.eventType(), time);
        TrackingEvent saved = eventRepository.save(event);

        applyStatusFromEvent(s, req.eventType(), time);
        return TrackingEventResponse.from(saved);
    }

    /** Keep the shipment status in sync with the latest scan. */
    private void applyStatusFromEvent(Shipment s, EventType type, OffsetDateTime time) {
        switch (type) {
            case PICKED_UP, ARRIVED_AT_HUB, DEPARTED_HUB -> s.setStatus(ShipmentStatus.IN_TRANSIT);
            case OUT_FOR_DELIVERY -> s.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);
            case EXCEPTION -> s.setStatus(ShipmentStatus.EXCEPTION);
            case DELIVERED -> {
                s.setStatus(ShipmentStatus.DELIVERED);
                s.setDeliveredAt(time);
            }
        }
    }

    // ----------------------------------------------------------- ML enrichment

    /**
     * Build the feature vector for one shipment and ask the ML service for a
     * delay prediction. Returns a fallback result if the service is unavailable.
     */
    private PredictionResult predictDelay(Shipment s) {
        double distanceKm = routeService.shortestDistanceKm(
                s.getOriginHub().getCode(), s.getDestHub().getCode());

        OffsetDateTime basis = s.getCreatedAt();
        int dayOfWeek = basis.getDayOfWeek().getValue() - 1; // Java 1..7 (Mon..Sun) -> 0..6
        int month = basis.getMonthValue();
        boolean festival = (month == 10 || month == 11);     // Diwali / festival season
        boolean monsoon = (month >= 6 && month <= 9);        // SW monsoon
        int hubLoad = (int) Math.min(100, shipmentRepository.countByOriginHubId(s.getOriginHub().getId()));

        MlPredictRequest features = new MlPredictRequest(
                distanceKm,
                s.getWeightKg().doubleValue(),
                s.getServiceType().name(),
                dayOfWeek,
                month,
                festival,
                monsoon,
                hubLoad
        );
        return mlPredictionClient.predict(features);
    }

    // --------------------------------------------------------------- helpers

    private Shipment requireById(Integer id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shipment not found: id=" + id));
    }

    private Shipment requireByTrackingNo(String trackingNo) {
        return shipmentRepository.findByTrackingNo(trackingNo)
                .orElseThrow(() -> new NotFoundException("Shipment not found: " + trackingNo));
    }

    /** Generate a unique tracking number like "SS" + 10 digits. */
    private String generateTrackingNo() {
        String candidate;
        do {
            long n = ThreadLocalRandom.current().nextLong(0, 10_000_000_000L);
            candidate = String.format("SS%010d", n);
        } while (shipmentRepository.existsByTrackingNo(candidate));
        return candidate;
    }

    private static String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }
}
