package com.shipsense.exception;

/** Thrown when no path exists between two hubs -> HTTP 422 (Unprocessable Entity). */
public class NoRouteFoundException extends RuntimeException {
    public NoRouteFoundException(String fromCode, String toCode) {
        super("No route found from '" + fromCode + "' to '" + toCode + "'");
    }
}
