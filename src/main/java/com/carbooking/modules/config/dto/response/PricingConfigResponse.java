package com.carbooking.modules.config.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PricingConfigResponse {
    private UUID id;
    private UUID tenantId;
    private BigDecimal baseFare;
    private BigDecimal perKmRate;
    private BigDecimal perHourRate;
    private BigDecimal minimumFare;
    private BigDecimal sedanMultiplier;
    private BigDecimal suvMultiplier;
    private BigDecimal luxuryMultiplier;
    private BigDecimal cgstPct;
    private BigDecimal sgstPct;
    private boolean active;
    private Instant effectiveFrom;
    private Instant updatedAt;
}
