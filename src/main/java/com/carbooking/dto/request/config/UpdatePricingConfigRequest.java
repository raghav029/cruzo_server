package com.carbooking.dto.request.config;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class UpdatePricingConfigRequest {

    @DecimalMin(value = "0.0")
    private BigDecimal baseFare;

    @DecimalMin(value = "0.0")
    private BigDecimal perKmRate;

    @DecimalMin(value = "0.0")
    private BigDecimal perHourRate;

    @DecimalMin(value = "0.0")
    private BigDecimal minimumFare;

    @DecimalMin(value = "0.0")
    private BigDecimal sedanMultiplier;

    @DecimalMin(value = "0.0")
    private BigDecimal suvMultiplier;

    @DecimalMin(value = "0.0")
    private BigDecimal luxuryMultiplier;

    @DecimalMin(value = "0.0")
    private BigDecimal cgstPct;

    @DecimalMin(value = "0.0")
    private BigDecimal sgstPct;

    private Instant effectiveFrom;
}
