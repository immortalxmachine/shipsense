package com.shipsense.exception;

/** Thrown when a requested resource (shipment, hub, ...) does not exist -> HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
