package com.carbooking.modules.recurring.application;

import com.carbooking.entity.RecurringBooking;
import com.carbooking.modules.recurring.dto.response.RecurringBookingResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class RecurringBookingMapper {

    public RecurringBookingResponse toResponse(RecurringBooking rb) {
        return RecurringBookingResponse.builder()
                .id(rb.getId())
                .tenantId(rb.getTenant().getId())
                .corporateClientId(rb.getCorporateClient().getId())
                .employeeUserId(rb.getEmployee().getId())
                .employeeName(rb.getEmployee().getFullName())
                .pickupAddress(rb.getPickupAddress())
                .dropAddress(rb.getDropAddress())
                .vehicleType(rb.getVehicleType())
                .scheduledTime(rb.getScheduledTime())
                .recurrenceDays(Arrays.asList(rb.getRecurrenceDays().split(",")))
                .isActive(rb.isActive())
                .notes(rb.getNotes())
                .createdAt(rb.getCreatedAt())
                .build();
    }
}
