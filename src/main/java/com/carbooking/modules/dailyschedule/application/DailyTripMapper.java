package com.carbooking.modules.dailyschedule.application;

import com.carbooking.entity.DailyTrip;
import com.carbooking.entity.DailyTripPassenger;
import com.carbooking.modules.dailyschedule.dto.response.DailyTripPassengerResponse;
import com.carbooking.modules.dailyschedule.dto.response.DailyTripResponse;
import com.carbooking.modules.dailyschedule.dto.response.MyTodayTripResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DailyTripMapper {

    public DailyTripResponse toTripResponse(DailyTrip trip, List<DailyTripPassengerResponse> passengers) {
        return DailyTripResponse.builder()
                .id(trip.getId())
                .dailyScheduleId(trip.getDailySchedule().getId())
                .scheduleName(trip.getDailySchedule().getName())
                .tripDate(trip.getTripDate())
                .scheduledPickupTime(trip.getScheduledPickupTime())
                .dropAddress(trip.getDropAddress())
                .driverId(trip.getDriver() != null ? trip.getDriver().getId() : null)
                .driverName(trip.getDriver() != null ? trip.getDriver().getUser().getFullName() : null)
                .driverPhone(trip.getDriver() != null ? trip.getDriver().getUser().getPhone() : null)
                .vehicleId(trip.getVehicle() != null ? trip.getVehicle().getId() : null)
                .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                .status(trip.getStatus())
                .passengers(passengers)
                .createdAt(trip.getCreatedAt())
                .build();
    }

    public DailyTripPassengerResponse toPassengerResponse(DailyTripPassenger p) {
        return DailyTripPassengerResponse.builder()
                .id(p.getId())
                .employeeUserId(p.getEmployee().getId())
                .employeeName(p.getEmployee().getFullName())
                .employeePhone(p.getEmployee().getPhone())
                .pickupAddress(p.getPickupAddress())
                .stopSequence(p.getStopSequence())
                .boardingOtp(p.getBoardingOtp())
                .dropOtp(p.getDropOtp())
                .status(p.getStatus())
                .boardingVerifiedAt(p.getBoardingVerifiedAt())
                .dropVerifiedAt(p.getDropVerifiedAt())
                .cancelledAt(p.getCancelledAt())
                .build();
    }

    public MyTodayTripResponse toMyTodayTripResponse(DailyTripPassenger p) {
        DailyTrip trip = p.getDailyTrip();
        return MyTodayTripResponse.builder()
                .tripId(trip.getId())
                .passengerId(p.getId())
                .scheduleName(trip.getDailySchedule().getName())
                .tripDate(trip.getTripDate())
                .pickupTime(trip.getScheduledPickupTime())
                .pickupAddress(p.getPickupAddress())
                .dropAddress(trip.getDropAddress())
                .driverName(trip.getDriver() != null ? trip.getDriver().getUser().getFullName() : null)
                .driverPhone(trip.getDriver() != null ? trip.getDriver().getUser().getPhone() : null)
                .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                .boardingOtp(p.getBoardingOtp())
                .dropOtp(p.getDropOtp())
                .passengerStatus(p.getStatus())
                .tripStatus(trip.getStatus())
                .build();
    }
}
