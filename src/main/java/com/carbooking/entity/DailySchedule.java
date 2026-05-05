package com.carbooking.entity;

import com.carbooking.common.enums.VehicleType;
import com.carbooking.common.util.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "daily_schedules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailySchedule extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corporate_client_id", nullable = false)
    private CorporateClient corporateClient;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "recurrence_days", nullable = false)
    private String recurrenceDays; // "MON,TUE,WED,THU,FRI"

    @Column(name = "pickup_time", nullable = false)
    private java.time.LocalTime pickupTime;

    @Column(name = "drop_address", nullable = false, columnDefinition = "TEXT")
    private String dropAddress;

    @Column(name = "drop_lat", precision = 9, scale = 6)
    private java.math.BigDecimal dropLat;

    @Column(name = "drop_lng", precision = 9, scale = 6)
    private java.math.BigDecimal dropLng;

    @Column(name = "is_pooled", nullable = false)
    private boolean isPooled = false;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity = 1;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;
}
