package com.carbooking.modules.client.application;

import com.carbooking.entity.CorporateClient;
import com.carbooking.modules.client.dto.response.CorporateClientResponse;
import org.springframework.stereotype.Component;

@Component
public class CorporateClientMapper {

    public CorporateClientResponse toResponse(CorporateClient c) {
        return CorporateClientResponse.builder()
                .id(c.getId())
                .tenantId(c.getTenant().getId())
                .companyName(c.getCompanyName())
                .gstNumber(c.getGstNumber())
                .billingAddress(c.getBillingAddress())
                .billingEmail(c.getBillingEmail())
                .billingCycle(c.getBillingCycle())
                .creditLimit(c.getCreditLimit())
                .currentOutstanding(c.getCurrentOutstanding())
                .active(c.isActive())
                .maxBookingValue(c.getMaxBookingValue())
                .allowedVehicleTypes(c.getAllowedVehicleTypes())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
