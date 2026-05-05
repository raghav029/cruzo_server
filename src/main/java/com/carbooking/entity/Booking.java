package com.carbooking.entity;

import com.carbooking.common.enums.AssignmentMode;
import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.enums.VehicleType;
import com.carbooking.common.util.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corporate_client_id", nullable = false)
    private CorporateClient corporateClient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_user_id", nullable = false)
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id")
    private User assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_mode")
    private AssignmentMode assignmentMode;

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "drop_address", nullable = false)
    private String dropAddress;

    @Column(name = "pickup_lat", precision = 9, scale = 6)
    private BigDecimal pickupLat;

    @Column(name = "pickup_lng", precision = 9, scale = 6)
    private BigDecimal pickupLng;

    @Column(name = "drop_lat", precision = 9, scale = 6)
    private BigDecimal dropLat;

    @Column(name = "drop_lng", precision = 9, scale = 6)
    private BigDecimal dropLng;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type_requested", nullable = false)
    private VehicleType vehicleTypeRequested;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.DRAFT;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "estimated_fare", precision = 10, scale = 2)
    private BigDecimal estimatedFare;

    @Column(name = "final_fare", precision = 10, scale = 2)
    private BigDecimal finalFare;

    @Column(name = "cancellation_fee", precision = 10, scale = 2)
    private BigDecimal cancellationFee;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "driver_assigned_at")
    private Instant driverAssignedAt;

    @Column(name = "trip_started_at")
    private Instant tripStartedAt;

    @Column(name = "trip_completed_at")
    private Instant tripCompletedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "driver_current_lat", precision = 9, scale = 6)
    private BigDecimal driverCurrentLat;

    @Column(name = "driver_current_lng", precision = 9, scale = 6)
    private BigDecimal driverCurrentLng;

    @Column(name = "location_updated_at")
    private Instant locationUpdatedAt;

    @Column(name = "drop_otp")
    private String dropOtp;

    @Column(name = "otp_generated_at")
    private Instant otpGeneratedAt;

    @Column(name = "otp_verified_at")
    private Instant otpVerifiedAt;
}
