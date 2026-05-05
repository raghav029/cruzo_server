package com.carbooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "daily_trip_skip_dates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyTripSkipDate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_schedule_passenger_id", nullable = false)
    private DailySchedulePassenger schedulePassenger;

    @Column(name = "skip_date", nullable = false)
    private java.time.LocalDate skipDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skipped_by_user_id")
    private User skippedBy;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt = java.time.Instant.now();
}
