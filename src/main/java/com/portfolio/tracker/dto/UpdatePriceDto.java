package com.portfolio.tracker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * DTO for updating the current market price of an asset.
 */
public class UpdatePriceDto {

    @NotNull(message = "price is required")
    @Positive(message = "price must be greater than 0")
    private BigDecimal price;

    public BigDecimal getPrice()              { return price;  }
    public void setPrice(BigDecimal price)    { this.price = price; }
}
