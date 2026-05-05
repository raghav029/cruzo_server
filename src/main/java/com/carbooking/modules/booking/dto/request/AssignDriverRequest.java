package com.carbooking.modules.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AssignDriverRequest {

    @NotNull(message = "Driver ID is required")
    private UUID driverId;

    @NotNull(message = "Vehicle ID is required")
    private UUID vehicleId;
}
