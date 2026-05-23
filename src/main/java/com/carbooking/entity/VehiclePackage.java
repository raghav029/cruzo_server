package com.carbooking.entity;

import com.carbooking.common.util.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vehicle_packages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VehiclePackage extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "base_rental", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseRental;

    @Column(name = "included_km", nullable = false)
    private Integer includedKm = 0;

    @Column(name = "included_hours", nullable = false)
    private Integer includedHours = 0;

    @Column(name = "extra_per_km", nullable = false, precision = 10, scale = 2)
    private BigDecimal extraPerKm = BigDecimal.ZERO;

    @Column(name = "extra_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal extraPerHour = BigDecimal.ZERO;

    @Column(name = "drive_batta", nullable = false, precision = 10, scale = 2)
    private BigDecimal driveBatta = BigDecimal.ZERO;

    @Column(name = "outstation_batta", nullable = false, precision = 10, scale = 2)
    private BigDecimal outstationBatta = BigDecimal.ZERO;

    @Column(name = "night_batta", nullable = false, precision = 10, scale = 2)
    private BigDecimal nightBatta = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
