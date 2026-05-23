package com.carbooking.dto.request.b2c;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter
public class CreateB2CBookingRequest {
    @NotNull(message = "Vehicle is required")
    private UUID vehicleId;

    @NotNull(message = "Package is required")
    private UUID packageId;

    @NotNull(message = "Scheduled time is required")
    @Future(message = "Scheduled time must be in the future")
    private Instant scheduledAt;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotBlank(message = "Drop address is required")
    private String dropAddress;

    private String notes;
    private boolean outstationTrip = false;
    private String promoCode;
    private UUID cityId;
}
