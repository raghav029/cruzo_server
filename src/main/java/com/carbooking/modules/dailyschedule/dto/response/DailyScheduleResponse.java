package com.carbooking.modules.dailyschedule.dto.response;

import com.carbooking.common.enums.VehicleType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter @Builder
public class DailyScheduleResponse {
    private UUID id;
    private UUID tenantId;
    private UUID corporateClientId;
    private String corporateClientName;
    private String name;
    private VehicleType vehicleType;
    private List<String> recurrenceDays;
    private java.time.LocalTime pickupTime;
    private String dropAddress;
    private boolean isPooled;
    private Integer maxCapacity;
    private boolean isActive;
    private Integer enrolledPassengerCount;
    private Instant createdAt;
}
