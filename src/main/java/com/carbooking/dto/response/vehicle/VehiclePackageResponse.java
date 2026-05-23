package com.carbooking.dto.response.vehicle;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Builder
public class VehiclePackageResponse {
    private UUID id;
    private String name;
    private BigDecimal baseRental;
    private Integer includedKm;
    private Integer includedHours;
    private BigDecimal extraPerKm;
    private BigDecimal extraPerHour;
    private BigDecimal driveBatta;
    private BigDecimal outstationBatta;
    private BigDecimal nightBatta;
}
