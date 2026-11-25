package com.shipsense.exception;

/** Thrown for invalid business input not caught by bean validation -> HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
