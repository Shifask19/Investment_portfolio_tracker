package com.portfolio.tracker.exception;

/**
 * Thrown when a transaction is attempted for a deactivated client.
 */
public class ClientInactiveException extends RuntimeException {

    public ClientInactiveException(Long clientId) {
        super("Client with id " + clientId + " is INACTIVE and cannot perform transactions.");
    }
}
