package com.carbooking.entity;

import com.carbooking.common.enums.CancellationFeeType;
import com.carbooking.common.util.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cancellation_configs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CancellationConfig extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    @Column(name = "cancellation_window_hours", nullable = false, precision = 4, scale = 1)
    private BigDecimal cancellationWindowHours = new BigDecimal("2.0");

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false)
    private CancellationFeeType feeType = CancellationFeeType.FLAT;

    @Column(name = "fee_value", nullable = false, precision = 8, scale = 2)
    private BigDecimal feeValue = BigDecimal.ZERO;

    @Column(name = "after_window_allowed", nullable = false)
    private boolean afterWindowAllowed = false;

    @Column(name = "skip_cutoff_hour", nullable = false)
    private Integer skipCutoffHour = 22;
}
