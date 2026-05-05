package com.carbooking.dto.response.document;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class DriverExpiryItem {
    private UUID driverId;
    private String driverName;
    private String phone;
    private LocalDate expiryDate;
    private long daysUntilExpiry;
}
