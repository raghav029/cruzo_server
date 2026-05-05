package com.carbooking.modules.booking.dto.response;

import com.carbooking.common.enums.AssignmentMode;
import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.enums.VehicleType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class BookingResponse {
    private UUID id;
    private UUID tenantId;
    private UUID corporateClientId;
    private String corporateClientName;
    private UUID employeeUserId;
    private String employeeName;
    private UUID driverId;
    private String driverName;
    private UUID vehicleId;
    private String vehiclePlate;
    private AssignmentMode assignmentMode;
    private String pickupAddress;
    private String dropAddress;
    private BigDecimal pickupLat;
    private BigDecimal pickupLng;
    private BigDecimal dropLat;
    private BigDecimal dropLng;
    private VehicleType vehicleTypeRequested;
    private Instant scheduledAt;
    private String notes;
    private BookingStatus status;
    private String cancellationReason;
    private String rejectionReason;
    private BigDecimal estimatedFare;
    private BigDecimal finalFare;
    private BigDecimal cancellationFee;
    private Instant approvedAt;
    private Instant driverAssignedAt;
    private Instant tripStartedAt;
    private Instant tripCompletedAt;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;
    private BigDecimal driverCurrentLat;
    private BigDecimal driverCurrentLng;
    private Instant locationUpdatedAt;
    private Instant otpVerifiedAt;
}
