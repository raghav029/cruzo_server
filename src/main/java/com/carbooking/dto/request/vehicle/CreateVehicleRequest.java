package com.carbooking.dto.request.vehicle;

import com.carbooking.common.enums.VehicleType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateVehicleRequest {

    @NotBlank(message = "Plate number is required")
    private String plateNumber;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotBlank(message = "Make is required")
    private String make;

    @NotBlank(message = "Model is required")
    private String model;

    @Min(value = 2000, message = "Year must be 2000 or later")
    @Max(value = 2100, message = "Year is invalid")
    private Short year;

    private String color;
    private LocalDate insuranceExpiry;
    private LocalDate fitnessExpiry;
}
