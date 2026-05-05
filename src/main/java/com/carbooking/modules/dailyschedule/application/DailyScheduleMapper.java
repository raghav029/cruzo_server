package com.carbooking.modules.dailyschedule.application;

import com.carbooking.entity.DailySchedule;
import com.carbooking.entity.DailySchedulePassenger;
import com.carbooking.modules.dailyschedule.dto.response.DailySchedulePassengerResponse;
import com.carbooking.modules.dailyschedule.dto.response.DailyScheduleResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DailyScheduleMapper {

    public DailyScheduleResponse toResponse(DailySchedule schedule, int enrolledCount) {
        return DailyScheduleResponse.builder()
                .id(schedule.getId())
                .tenantId(schedule.getTenant().getId())
                .corporateClientId(schedule.getCorporateClient().getId())
                .corporateClientName(schedule.getCorporateClient().getCompanyName())
                .name(schedule.getName())
                .vehicleType(schedule.getVehicleType())
                .recurrenceDays(Arrays.asList(schedule.getRecurrenceDays().split(",")))
                .pickupTime(schedule.getPickupTime())
                .dropAddress(schedule.getDropAddress())
                .isPooled(schedule.isPooled())
                .maxCapacity(schedule.getMaxCapacity())
                .isActive(schedule.isActive())
                .enrolledPassengerCount(enrolledCount)
                .createdAt(schedule.getCreatedAt())
                .build();
    }

    public DailySchedulePassengerResponse toPassengerResponse(DailySchedulePassenger p) {
        return DailySchedulePassengerResponse.builder()
                .id(p.getId())
                .employeeUserId(p.getEmployee().getId())
                .employeeName(p.getEmployee().getFullName())
                .employeeEmail(p.getEmployee().getEmail())
                .employeePhone(p.getEmployee().getPhone())
                .pickupAddress(p.getPickupAddress())
                .pickupLat(p.getPickupLat())
                .pickupLng(p.getPickupLng())
                .stopSequence(p.getStopSequence())
                .isActive(p.isActive())
                .enrolledAt(p.getEnrolledAt())
                .sequenceAssignedAt(p.getSequenceAssignedAt())
                .build();
    }
}
