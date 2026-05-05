package com.carbooking.modules.fleet.dto.response;

import com.carbooking.common.enums.DriverAvailability;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class DriverResponse {

    private UUID id;
    private UUID tenantId;
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private String licenseNumber;
    private LocalDate licenseExpiry;
    private LocalDate insuranceExpiry;
    private DriverAvailability availability;
    private UUID currentVehicleId;
    private Instant createdAt;
    private Instant updatedAt;
}
