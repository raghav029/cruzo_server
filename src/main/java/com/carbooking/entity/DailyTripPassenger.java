package com.carbooking.entity;

import com.carbooking.common.enums.DailyTripPassengerStatus;
import com.carbooking.common.util.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "daily_trip_passengers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyTripPassenger extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_trip_id", nullable = false)
    private DailyTrip dailyTrip;

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
    private Integer stopSequence;

    @Column(name = "boarding_otp", nullable = false)
    private String boardingOtp;

    @Column(name = "drop_otp", nullable = false)
    private String dropOtp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DailyTripPassengerStatus status = DailyTripPassengerStatus.SCHEDULED;

    @Column(name = "boarding_verified_at")
    private java.time.Instant boardingVerifiedAt;

    @Column(name = "drop_verified_at")
    private java.time.Instant dropVerifiedAt;

    @Column(name = "cancelled_at")
    private java.time.Instant cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "otp_sms_sent", nullable = false)
    private boolean otpSmsSent = false;
}
