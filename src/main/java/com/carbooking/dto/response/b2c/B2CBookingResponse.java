package com.carbooking.dto.response.b2c;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.dto.response.vehicle.VehiclePackageResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Builder
public class B2CBookingResponse {
    private UUID id;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleColor;
    private String thumbnailUrl;
    private VehiclePackageResponse packageDetails;
    private String pickupAddress;
    private String dropAddress;
    private Instant scheduledAt;
    private BookingStatus status;
    private String notes;
    private BigDecimal baseRental;
    private BigDecimal extraKm;
    private BigDecimal extraHours;
    private BigDecimal driveBattaApplied;
    private BigDecimal outstationBattaApplied;
    private BigDecimal nightBattaApplied;
    private BigDecimal parkingFee;
    private BigDecimal tollFee;
    private BigDecimal gstAmount;
    private BigDecimal estimatedFare;
    private BigDecimal finalFare;
    private Instant createdAt;
}
