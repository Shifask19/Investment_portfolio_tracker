package com.portfolio.tracker.dao.impl;

import com.portfolio.tracker.dao.ClientDao;
import com.portfolio.tracker.model.Client;
import com.portfolio.tracker.model.enums.ClientStatus;
import com.portfolio.tracker.model.enums.RiskProfile;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of ClientDao.
 *
 * Key patterns used here:
 *  1. PreparedStatement — prevents SQL injection; always preferred over string concatenation.
 *  2. try-with-resources — automatically closes Connection/Statement/ResultSet,
 *     avoiding resource leaks even if an exception is thrown.
 *  3. DataSource injection — HikariCP provides the connection pool;
 *     we never manage connections manually.
 */
@Repository  // marks this as a Spring-managed bean; triggers component scan
public class ClientDaoImpl implements ClientDao {

    /** HikariCP-backed DataSource injected by Spring. */
    private final DataSource dataSource;

    // Constructor injection is preferred over @Autowired on fields —
    // it makes dependencies explicit and the class easier to test.
    public ClientDaoImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ─────────────────────────────────────────────────────────────
    // SAVE
    // ─────────────────────────────────────────────────────────────

    @Override
    public Client save(Client client) {
        String sql = "INSERT INTO clients (name, email, phone, risk_profile, status) "
                   + "VALUES (?, ?, ?, ?, ?)";

        // RETURN_GENERATED_KEYS tells JDBC to give us the auto-incremented PK.
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, client.getName());
            ps.setString(2, client.getEmail());
            ps.setString(3, client.getPhone());
            ps.setString(4, client.getRiskProfile().name());
            ps.setString(5, client.getStatus() != null
                             ? client.getStatus().name()
                             : ClientStatus.ACTIVE.name());

            ps.executeUpdate();

            // Retrieve the generated primary key
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    client.setClientId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save client: " + e.getMessage(), e);
        }
        return client;
    }

    // ─────────────────────────────────────────────────────────────
    // FIND BY ID
    // ─────────────────────────────────────────────────────────────

    @Override
    public Optional<Client> findById(Long clientId) {
        String sql = "SELECT client_id, name, email, phone, risk_profile, status, created_at "
                   + "FROM clients WHERE client_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, clientId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find client by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ─────────────────────────────────────────────────────────────
    // FIND BY EMAIL
    // ─────────────────────────────────────────────────────────────

    @Override
    public Optional<Client> findByEmail(String email) {
        String sql = "SELECT client_id, name, email, phone, risk_profile, status, created_at "
                   + "FROM clients WHERE email = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find client by email: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ─────────────────────────────────────────────────────────────
    // FIND ALL
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<Client> findAll() {
        String sql = "SELECT client_id, name, email, phone, risk_profile, status, created_at "
                   + "FROM clients ORDER BY client_id";

        List<Client> clients = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                clients.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all clients: " + e.getMessage(), e);
        }
        return clients;
    }

    // ─────────────────────────────────────────────────────────────
    // FIND ALL ACTIVE
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<Client> findAllActive() {
        String sql = "SELECT client_id, name, email, phone, risk_profile, status, created_at "
                   + "FROM clients WHERE status = 'ACTIVE' ORDER BY client_id";

        List<Client> clients = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                clients.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch active clients: " + e.getMessage(), e);
        }
        return clients;
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────

    @Override
    public void update(Client client) {
        String sql = "UPDATE clients SET name = ?, phone = ?, risk_profile = ?, status = ? "
                   + "WHERE client_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, client.getName());
            ps.setString(2, client.getPhone());
            ps.setString(3, client.getRiskProfile().name());
            ps.setString(4, client.getStatus().name());
            ps.setLong(5, client.getClientId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update client: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DEACTIVATE (soft delete)
    // ─────────────────────────────────────────────────────────────

    @Override
    public void deactivate(Long clientId) {
        String sql = "UPDATE clients SET status = 'INACTIVE' WHERE client_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, clientId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to deactivate client: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPER: map one ResultSet row → Client object
    // ─────────────────────────────────────────────────────────────

    /**
     * Converts a single ResultSet row into a Client domain object.
     * Called from every SELECT method — DRY principle.
     */
    private Client mapRow(ResultSet rs) throws SQLException {
        Client c = new Client();
        c.setClientId(rs.getLong("client_id"));
        c.setName(rs.getString("name"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        c.setRiskProfile(RiskProfile.valueOf(rs.getString("risk_profile")));
        c.setStatus(ClientStatus.valueOf(rs.getString("status")));
        // rs.getTimestamp() returns null-safe; convert to LocalDateTime
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            c.setCreatedAt(ts.toLocalDateTime());
        }
        return c;
    }
}
