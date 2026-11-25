package com.shipsense.repository;

import com.shipsense.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Integer> {

    Optional<Shipment> findByTrackingNo(String trackingNo);

    boolean existsByTrackingNo(String trackingNo);

    /** Outbound shipment count for a hub — a simple proxy for "hub load". */
    long countByOriginHubId(Integer hubId);
}
