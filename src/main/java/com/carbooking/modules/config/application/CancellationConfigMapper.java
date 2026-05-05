package com.carbooking.modules.config.application;

import com.carbooking.entity.CancellationConfig;
import com.carbooking.modules.config.dto.response.CancellationConfigResponse;
import org.springframework.stereotype.Component;

@Component
public class CancellationConfigMapper {

    public CancellationConfigResponse toResponse(CancellationConfig c) {
        return CancellationConfigResponse.builder()
                .id(c.getId())
                .tenantId(c.getTenant().getId())
                .cancellationWindowHours(c.getCancellationWindowHours())
                .feeType(c.getFeeType())
                .feeValue(c.getFeeValue())
                .afterWindowAllowed(c.isAfterWindowAllowed())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
