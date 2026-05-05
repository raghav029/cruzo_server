package com.carbooking.entity;

import com.carbooking.common.util.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pricing_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PricingConfig extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    @Column(name = "base_fare", nullable = false, precision = 8, scale = 2)
    private BigDecimal baseFare = BigDecimal.ZERO;

    @Column(name = "per_km_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal perKmRate = BigDecimal.ZERO;

    @Column(name = "per_hour_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal perHourRate = BigDecimal.ZERO;

    @Column(name = "minimum_fare", nullable = false, precision = 8, scale = 2)
    private BigDecimal minimumFare = BigDecimal.ZERO;

    @Column(name = "sedan_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal sedanMultiplier = BigDecimal.ONE;

    @Column(name = "suv_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal suvMultiplier = new BigDecimal("1.25");

    @Column(name = "luxury_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal luxuryMultiplier = new BigDecimal("1.75");

    @Column(name = "cgst_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal cgstPct = new BigDecimal("9.00");

    @Column(name = "sgst_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal sgstPct = new BigDecimal("9.00");

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom = Instant.now();
}
