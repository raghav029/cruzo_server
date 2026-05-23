package com.carbooking.dto.request.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class AddVehiclePackageRequest {
    @NotBlank(message = "Package name is required")
    private String name;

    @NotNull(message = "Base rental is required")
    private BigDecimal baseRental;

    private Integer includedKm = 0;
    private Integer includedHours = 0;
    private BigDecimal extraPerKm = BigDecimal.ZERO;
    private BigDecimal extraPerHour = BigDecimal.ZERO;
    private BigDecimal driveBatta = BigDecimal.ZERO;
    private BigDecimal outstationBatta = BigDecimal.ZERO;
    private BigDecimal nightBatta = BigDecimal.ZERO;
}
