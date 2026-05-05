package com.carbooking.modules.dailyschedule.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class AssignDriverToDailyTripRequest {
    @NotNull private UUID driverId;
    @NotNull private UUID vehicleId;
}
