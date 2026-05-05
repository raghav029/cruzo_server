package com.carbooking.entity;

import com.carbooking.common.util.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "daily_schedule_passengers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailySchedulePassenger extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_schedule_id", nullable = false)
    private DailySchedule dailySchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_user_id", nullable = false)
    private User employee;

    @Column(name = "pickup_address", nullable = false, columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(name = "pickup_lat", precision = 9, scale = 6)
    private java.math.BigDecimal pickupLat;

    @Column(name = "pickup_lng", precision = 9, scale = 6)
    private java.math.BigDecimal pickupLng;

    @Column(name = "stop_sequence")
    private Integer stopSequence; // NULL until fleet manager assigns

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrolled_by_user_id")
    private User enrolledBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sequence_assigned_by_user_id")
    private User sequenceAssignedBy;

    @Column(name = "sequence_assigned_at")
    private java.time.Instant sequenceAssignedAt;

    @Column(name = "enrolled_at", nullable = false)
    private java.time.Instant enrolledAt = java.time.Instant.now();
}
