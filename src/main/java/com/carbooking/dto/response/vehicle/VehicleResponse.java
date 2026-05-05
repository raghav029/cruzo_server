package com.carbooking.dto.response.vehicle;

import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.common.enums.VehicleType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class VehicleResponse {

    private UUID id;
    private UUID tenantId;
    private String plateNumber;
    private VehicleType vehicleType;
    private String make;
    private String model;
    private Short year;
    private String color;
    private VehicleStatus status;
    private LocalDate insuranceExpiry;
    private LocalDate fitnessExpiry;
    private Instant createdAt;
    private Instant updatedAt;
}
