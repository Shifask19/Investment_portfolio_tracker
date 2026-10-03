package com.portfolio.tracker.exception;

/**
 * Thrown when a requested client does not exist in the database.
 * Extends RuntimeException so callers don't need checked-exception handling,
 * keeping service/controller code clean.
 */
public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(Long clientId) {
        super("Client not found with id: " + clientId);
    }

    public ClientNotFoundException(String message) {
        super(message);
    }
}
