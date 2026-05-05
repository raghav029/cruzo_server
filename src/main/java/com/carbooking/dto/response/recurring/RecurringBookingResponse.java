package com.carbooking.dto.response.recurring;

import com.carbooking.common.enums.VehicleType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class RecurringBookingResponse {
    private UUID id;
    private UUID tenantId;
    private UUID corporateClientId;
    private UUID employeeUserId;
    private String employeeName;
    private String pickupAddress;
    private String dropAddress;
    private VehicleType vehicleType;
    private LocalTime scheduledTime;
    private List<String> recurrenceDays;
    private boolean isActive;
    private String notes;
    private Instant createdAt;
}
