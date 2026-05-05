package com.carbooking.modules.dailyschedule.dto.response;

import com.carbooking.common.enums.DailyTripStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter @Builder
public class DailyTripResponse {
    private UUID id;
    private UUID dailyScheduleId;
    private String scheduleName;
    private java.time.LocalDate tripDate;
    private java.time.LocalTime scheduledPickupTime;
    private String dropAddress;
    private UUID driverId;
    private String driverName;
    private String driverPhone;
    private UUID vehicleId;
    private String vehiclePlate;
    private DailyTripStatus status;
    private List<DailyTripPassengerResponse> passengers; // ordered by stopSequence
    private Instant createdAt;
}
