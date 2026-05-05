package com.carbooking.modules.dailyschedule.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter @Builder
public class DailySchedulePassengerResponse {
    private UUID id;
    private UUID employeeUserId;
    private String employeeName;
    private String employeeEmail;
    private String employeePhone;
    private String pickupAddress;
    private java.math.BigDecimal pickupLat;
    private java.math.BigDecimal pickupLng;
    private Integer stopSequence; // null if not yet assigned
    private boolean isActive;
    private Instant enrolledAt;
    private Instant sequenceAssignedAt;
}
