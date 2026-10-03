package com.portfolio.tracker.dto;

import com.portfolio.tracker.model.enums.RiskProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO (Data Transfer Object) for client creation and update requests.
 *
 * Why a separate DTO instead of using the domain model directly?
 *  - Decouples the API contract from the database model.
 *  - We can add/remove fields from the DTO without affecting the DB schema.
 *  - Bean Validation annotations live here, keeping the domain model clean.
 */
public class ClientRequestDto {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone must be 10-15 digits")
    private String phone;  // optional field

    @NotNull(message = "Risk profile is required (CONSERVATIVE, MODERATE, AGGRESSIVE)")
    private RiskProfile riskProfile;

    // ── Getters & Setters ─────────────────────────────────────────

    public String getName()                        { return name;        }
    public void setName(String name)               { this.name = name;   }

    public String getEmail()                       { return email;       }
    public void setEmail(String email)             { this.email = email; }

    public String getPhone()                       { return phone;       }
    public void setPhone(String phone)             { this.phone = phone; }

    public RiskProfile getRiskProfile()            { return riskProfile;           }
    public void setRiskProfile(RiskProfile riskProfile) { this.riskProfile = riskProfile; }
}
