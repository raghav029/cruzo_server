package com.carbooking.modules.document.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class VehicleExpiryItem {
    private UUID vehicleId;
    private String plateNumber;
    private String make;
    private String model;
    private LocalDate expiryDate;
    private long daysUntilExpiry;
}
