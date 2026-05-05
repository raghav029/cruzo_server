package com.carbooking.dto.request.booking;

import com.carbooking.common.enums.VehicleType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class CreateBookingRequest {

    @NotNull(message = "Corporate client is required")
    private UUID corporateClientId;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotBlank(message = "Drop address is required")
    private String dropAddress;

    private BigDecimal pickupLat;
    private BigDecimal pickupLng;
    private BigDecimal dropLat;
    private BigDecimal dropLng;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleTypeRequested;

    @NotNull(message = "Scheduled time is required")
    private Instant scheduledAt;

    private String notes;
}
