package com.carbooking.modules.fleet.application;

import com.carbooking.entity.Driver;
import com.carbooking.modules.fleet.dto.response.DriverResponse;
import org.springframework.stereotype.Component;

@Component
public class DriverMapper {

    public DriverResponse toResponse(Driver d) {
        return DriverResponse.builder()
                .id(d.getId())
                .tenantId(d.getTenant().getId())
                .userId(d.getUser().getId())
                .fullName(d.getUser().getFullName())
                .email(d.getUser().getEmail())
                .phone(d.getUser().getPhone())
                .licenseNumber(d.getLicenseNumber())
                .licenseExpiry(d.getLicenseExpiry())
                .insuranceExpiry(d.getInsuranceExpiry())
                .availability(d.getAvailability())
                .currentVehicleId(d.getCurrentVehicle() != null ? d.getCurrentVehicle().getId() : null)
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
