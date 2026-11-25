package com.shipsense.entity;

/** The kind of scan recorded as a parcel moves through the network. */
public enum EventType {
    PICKED_UP,
    ARRIVED_AT_HUB,
    DEPARTED_HUB,
    OUT_FOR_DELIVERY,
    DELIVERED,
    EXCEPTION
}
