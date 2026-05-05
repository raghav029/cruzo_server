package com.carbooking.dto.response.dailyschedule;

import com.carbooking.common.enums.DailyTripPassengerStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter @Builder
public class DailyTripPassengerResponse {
    private UUID id;
    private UUID employeeUserId;
    private String employeeName;
    private String employeePhone;
    private String pickupAddress;
    private Integer stopSequence;
    private String boardingOtp;  // always included — employee and driver both see this
    private String dropOtp;      // always included
    private DailyTripPassengerStatus status;
    private Instant boardingVerifiedAt;
    private Instant dropVerifiedAt;
    private Instant cancelledAt;
}
