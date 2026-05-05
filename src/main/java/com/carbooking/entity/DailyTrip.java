package com.carbooking.entity;

import com.carbooking.common.enums.DailyTripStatus;
import com.carbooking.common.util.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "daily_trips")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyTrip extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_schedule_id", nullable = false)
    private DailySchedule dailySchedule;

    @Column(name = "trip_date", nullable = false)
    private java.time.LocalDate tripDate;

    @Column(name = "scheduled_pickup_time", nullable = false)
    private java.time.LocalTime scheduledPickupTime;

    @Column(name = "drop_address", nullable = false, columnDefinition = "TEXT")
    private String dropAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DailyTripStatus status = DailyTripStatus.SCHEDULED;

    @Column(name = "driver_alert_sent", nullable = false)
    private boolean driverAlertSent = false;
}
