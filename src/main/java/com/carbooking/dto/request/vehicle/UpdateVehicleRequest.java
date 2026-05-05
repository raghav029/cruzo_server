package com.carbooking.dto.request.vehicle;

import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.common.enums.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateVehicleRequest {

    private VehicleType vehicleType;
    private String make;
    private String model;

    @Min(value = 2000, message = "Year must be 2000 or later")
    @Max(value = 2100, message = "Year is invalid")
    private Short year;

    private String color;
    private VehicleStatus status;
    private LocalDate insuranceExpiry;
    private LocalDate fitnessExpiry;
}
