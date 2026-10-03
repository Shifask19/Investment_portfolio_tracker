package com.portfolio.tracker.controller;

import com.portfolio.tracker.dto.ClientRequestDto;
import com.portfolio.tracker.model.Client;
import com.portfolio.tracker.model.enums.ClientStatus;
import com.portfolio.tracker.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for client management.
 *
 * Endpoints:
 *   POST   /api/clients          — create a new client
 *   GET    /api/clients          — list all clients
 *   GET    /api/clients/{id}     — get one client
 *   PUT    /api/clients/{id}     — update a client
 *   DELETE /api/clients/{id}     — deactivate (soft-delete) a client
 */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final PortfolioService portfolioService;

    public ClientController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    // ── POST /api/clients ─────────────────────────────────────────

    /**
     * @Valid triggers Bean Validation on the DTO.
     * If validation fails, MethodArgumentNotValidException is thrown
     * and GlobalExceptionHandler returns a 400 with field errors.
     */
    @PostMapping
    public ResponseEntity<Client> createClient(@Valid @RequestBody ClientRequestDto dto) {
        Client client = mapDtoToClient(dto);
        client.setStatus(ClientStatus.ACTIVE);
        Client saved = portfolioService.saveClient(client);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── GET /api/clients ──────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<Client>> getAllClients() {
        return ResponseEntity.ok(portfolioService.getAllClients());
    }

    // ── GET /api/clients/{id} ─────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<Client> getClient(@PathVariable Long id) {
        return ResponseEntity.ok(portfolioService.getClient(id));
    }

    // ── PUT /api/clients/{id} ─────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<Client> updateClient(@PathVariable Long id,
                                               @Valid @RequestBody ClientRequestDto dto) {
        Client updatedData = mapDtoToClient(dto);
        Client result = portfolioService.updateClient(id, updatedData);
        return ResponseEntity.ok(result);
    }

    // ── DELETE /api/clients/{id} (soft-delete) ────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateClient(@PathVariable Long id) {
        portfolioService.deactivateClient(id);
        return ResponseEntity.noContent().build();  // 204 No Content
    }

    // ── Helper: DTO → domain model ────────────────────────────────

    private Client mapDtoToClient(ClientRequestDto dto) {
        Client c = new Client();
        c.setName(dto.getName());
        c.setEmail(dto.getEmail());
        c.setPhone(dto.getPhone());
        c.setRiskProfile(dto.getRiskProfile());
        return c;
    }
}
