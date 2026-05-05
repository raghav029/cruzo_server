package com.carbooking.modules.dailyschedule.dto.response;

import com.carbooking.common.enums.DailyTripPassengerStatus;
import com.carbooking.common.enums.DailyTripStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter @Builder
public class MyTodayTripResponse {
    private UUID tripId;
    private UUID passengerId;
    private String scheduleName;
    private java.time.LocalDate tripDate;
    private java.time.LocalTime pickupTime;
    private String pickupAddress;
    private String dropAddress;
    private String driverName;   // null if not assigned yet
    private String driverPhone;
    private String vehiclePlate;
    private String boardingOtp;  // employee's OTP — always visible
    private String dropOtp;
    private DailyTripPassengerStatus passengerStatus;
    private DailyTripStatus tripStatus;
}
