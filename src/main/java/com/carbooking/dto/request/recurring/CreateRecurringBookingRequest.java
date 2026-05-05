package com.carbooking.dto.request.recurring;

import com.carbooking.common.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateRecurringBookingRequest {

    @NotNull(message = "Corporate client ID is required")
    private UUID corporateClientId;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotBlank(message = "Drop address is required")
    private String dropAddress;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotNull(message = "Scheduled time is required")
    private LocalTime scheduledTime;

    @NotEmpty(message = "Recurrence days are required")
    private List<String> recurrenceDays;

    private String notes;
}
