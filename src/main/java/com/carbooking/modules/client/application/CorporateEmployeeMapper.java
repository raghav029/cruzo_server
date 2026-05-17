package com.carbooking.modules.client.application;

import com.carbooking.entity.CorporateEmployee;
import com.carbooking.modules.client.dto.response.CorporateEmployeeResponse;
import org.springframework.stereotype.Component;

@Component
public class CorporateEmployeeMapper {

    public CorporateEmployeeResponse toResponse(CorporateEmployee e) {
        return CorporateEmployeeResponse.builder()
                .id(e.getId())
                .tenantId(e.getTenant().getId())
                .corporateClientId(e.getCorporateClient().getId())
                .userId(e.getUser().getId())
                .fullName(e.getUser().getFullName())
                .email(e.getUser().getEmail())
                .phone(e.getUser().getPhone())
                .employeeCode(e.getEmployeeCode())
                .department(e.getDepartment())
                .designation(e.getDesignation())
                .monthlyRideLimit(e.getMonthlyRideLimit())
                .active(e.isActive())
                .maxBookingValueOverride(e.getMaxBookingValueOverride())
                .allowedVehicleTypesOverride(e.getAllowedVehicleTypesOverride())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
