package com.portfolio.tracker.model;

import com.portfolio.tracker.model.enums.ClientStatus;
import com.portfolio.tracker.model.enums.RiskProfile;

import java.time.LocalDateTime;

/**
 * Domain model representing an investor / client.
 *
 * OOP Principle – Encapsulation:
 *   All fields are private; access is only through getters/setters.
 *   This ensures any future validation logic lives in one place.
 */
public class Client {

    private Long          clientId;
    private String        name;
    private String        email;
    private String        phone;
    private RiskProfile   riskProfile;
    private ClientStatus  status;
    private LocalDateTime createdAt;

    // ── Constructors ─────────────────────────────────────────────

    public Client() {}

    public Client(Long clientId, String name, String email, String phone,
                  RiskProfile riskProfile, ClientStatus status, LocalDateTime createdAt) {
        this.clientId    = clientId;
        this.name        = name;
        this.email       = email;
        this.phone       = phone;
        this.riskProfile = riskProfile;
        this.status      = status;
        this.createdAt   = createdAt;
    }

    // ── Getters & Setters ─────────────────────────────────────────

    public Long getClientId()               { return clientId;    }
    public void setClientId(Long clientId)  { this.clientId = clientId; }

    public String getName()                 { return name;  }
    public void setName(String name)        { this.name = name; }

    public String getEmail()                { return email; }
    public void setEmail(String email)      { this.email = email; }

    public String getPhone()                { return phone; }
    public void setPhone(String phone)      { this.phone = phone; }

    public RiskProfile getRiskProfile()                    { return riskProfile;  }
    public void setRiskProfile(RiskProfile riskProfile)    { this.riskProfile = riskProfile; }

    public ClientStatus getStatus()                        { return status;  }
    public void setStatus(ClientStatus status)             { this.status = status; }

    public LocalDateTime getCreatedAt()                    { return createdAt;  }
    public void setCreatedAt(LocalDateTime createdAt)      { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Client{clientId=" + clientId + ", name='" + name + "', email='" + email + "'}";
    }
}
