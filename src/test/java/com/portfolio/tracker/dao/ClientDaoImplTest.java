package com.portfolio.tracker.dao;

import com.portfolio.tracker.dao.impl.ClientDaoImpl;
import com.portfolio.tracker.model.Client;
import com.portfolio.tracker.model.enums.ClientStatus;
import com.portfolio.tracker.model.enums.RiskProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for ClientDaoImpl using an H2 in-memory database.
 *
 * @JdbcTest:
 *   - Starts a minimal Spring context with only JDBC-related beans.
 *   - Uses the H2 in-memory database (configured via src/test/resources).
 *   - Rolls back each test to keep them independent.
 *
 * @Import(ClientDaoImpl.class):
 *   - JdbcTest doesn't auto-configure our @Repository beans,
 *     so we import the class we want to test.
 *
 * @Sql annotations run SQL scripts to set up / tear down test data.
 */
@JdbcTest
@Import(ClientDaoImpl.class)
class ClientDaoImplTest {

    @Autowired
    private ClientDao clientDao;

    // ─────────────────────────────────────────────────────────────
    // SAVE tests
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save: persists a new client and assigns a generated ID")
    void save_persistsClientAndAssignsId() {
        Client client = buildClient("John Doe", "john@test.com", "9001234567",
                                    RiskProfile.MODERATE);

        Client saved = clientDao.save(client);

        assertThat(saved.getClientId()).isNotNull().isPositive();
        assertThat(saved.getName()).isEqualTo("John Doe");
        assertThat(saved.getStatus()).isEqualTo(ClientStatus.ACTIVE);
    }

    // ─────────────────────────────────────────────────────────────
    // FIND BY ID tests
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById: returns client when it exists")
    void findById_returnsClientWhenExists() {
        Client saved = clientDao.save(
            buildClient("Jane Smith", "jane@test.com", "9002345678", RiskProfile.CONSERVATIVE));

        Optional<Client> found = clientDao.findById(saved.getClientId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("jane@test.com");
    }

    @Test
    @DisplayName("findById: returns empty Optional for non-existent client")
    void findById_returnsEmptyForMissingClient() {
        Optional<Client> found = clientDao.findById(9999L);
        assertThat(found).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────
    // FIND BY EMAIL tests
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByEmail: returns client for a valid email")
    void findByEmail_returnsClientForValidEmail() {
        clientDao.save(buildClient("Bob", "bob@test.com", "9003456789", RiskProfile.AGGRESSIVE));

        Optional<Client> found = clientDao.findByEmail("bob@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Bob");
    }

    // ─────────────────────────────────────────────────────────────
    // FIND ALL tests
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll: returns all saved clients")
    void findAll_returnsAllClients() {
        clientDao.save(buildClient("Client A", "a@test.com", "9000000001", RiskProfile.MODERATE));
        clientDao.save(buildClient("Client B", "b@test.com", "9000000002", RiskProfile.MODERATE));

        List<Client> all = clientDao.findAll();

        // At least 2 (there may be seeded data depending on test config)
        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    // ─────────────────────────────────────────────────────────────
    // UPDATE tests
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: changes mutable fields in the database")
    void update_changesMutableFields() {
        Client saved = clientDao.save(
            buildClient("Old Name", "oldname@test.com", "9004567890", RiskProfile.MODERATE));

        saved.setName("New Name");
        saved.setPhone("8888888888");
        saved.setRiskProfile(RiskProfile.AGGRESSIVE);
        clientDao.update(saved);

        Client reloaded = clientDao.findById(saved.getClientId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("New Name");
        assertThat(reloaded.getRiskProfile()).isEqualTo(RiskProfile.AGGRESSIVE);
    }

    // ─────────────────────────────────────────────────────────────
    // DEACTIVATE tests
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivate: sets status to INACTIVE")
    void deactivate_setsStatusToInactive() {
        Client saved = clientDao.save(
            buildClient("Active User", "active@test.com", "9005678901", RiskProfile.MODERATE));

        clientDao.deactivate(saved.getClientId());

        Client reloaded = clientDao.findById(saved.getClientId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(ClientStatus.INACTIVE);
    }

    @Test
    @DisplayName("findAllActive: does not include deactivated clients")
    void findAllActive_excludesInactiveClients() {
        Client active   = clientDao.save(buildClient("Active",   "ac@test.com",   "9006789012", RiskProfile.MODERATE));
        Client inactive = clientDao.save(buildClient("Inactive", "inac@test.com", "9007890123", RiskProfile.MODERATE));

        clientDao.deactivate(inactive.getClientId());

        List<Client> activeClients = clientDao.findAllActive();

        boolean hasInactive = activeClients.stream()
            .anyMatch(c -> c.getClientId().equals(inactive.getClientId()));
        assertThat(hasInactive).isFalse();
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────

    private Client buildClient(String name, String email, String phone, RiskProfile profile) {
        Client c = new Client();
        c.setName(name);
        c.setEmail(email);
        c.setPhone(phone);
        c.setRiskProfile(profile);
        c.setStatus(ClientStatus.ACTIVE);
        return c;
    }
}
