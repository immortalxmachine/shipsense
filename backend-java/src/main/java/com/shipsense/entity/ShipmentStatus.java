package com.shipsense.entity;

/** Lifecycle state of a shipment. Matches the CHECK constraint in schema.sql. */
public enum ShipmentStatus {
    CREATED,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    DELAYED,
    EXCEPTION
}
