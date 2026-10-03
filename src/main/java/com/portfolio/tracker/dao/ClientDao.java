package com.portfolio.tracker.dao;

import com.portfolio.tracker.model.Client;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Client persistence.
 *
 * OOP Principle – Abstraction / Interface:
 *   The service layer depends on this interface, NOT on the JDBC
 *   implementation. This makes the service testable with a mock DAO,
 *   and the implementation can be swapped (e.g., to JPA) without
 *   touching the business logic — Dependency Inversion Principle.
 */
public interface ClientDao {

    /** Persist a new client and return the created entity (with generated ID). */
    Client save(Client client);

    /** Find a client by primary key; returns empty Optional if not found. */
    Optional<Client> findById(Long clientId);

    /** Find a client by email; used for duplicate-email validation. */
    Optional<Client> findByEmail(String email);

    /** Return all clients, active and inactive. */
    List<Client> findAll();

    /** Return only active clients. */
    List<Client> findAllActive();

    /** Overwrite the mutable fields (name, phone, riskProfile, status). */
    void update(Client client);

    /** Soft-delete: sets status = INACTIVE instead of deleting the row. */
    void deactivate(Long clientId);
}
