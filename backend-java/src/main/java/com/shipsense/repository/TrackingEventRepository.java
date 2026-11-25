package com.shipsense.repository;

import com.shipsense.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Integer> {

    /** Chronological scan history for one shipment. */
    List<TrackingEvent> findByShipmentIdOrderByEventTimeAsc(Integer shipmentId);
}
