package com.carbooking.modules.sos.application;

import com.carbooking.entity.SosAlert;
import com.carbooking.modules.sos.dto.response.SosAlertResponse;
import org.springframework.stereotype.Component;

@Component
public class SosAlertMapper {

    public SosAlertResponse toResponse(SosAlert a) {
        return SosAlertResponse.builder()
                .id(a.getId())
                .tenantId(a.getTenant().getId())
                .bookingId(a.getBooking() != null ? a.getBooking().getId() : null)
                .triggeredByUserId(a.getTriggeredBy().getId())
                .triggeredByName(a.getTriggeredBy().getFullName())
                .lat(a.getLat())
                .lng(a.getLng())
                .message(a.getMessage())
                .status(a.getStatus())
                .resolvedByUserId(a.getResolvedBy() != null ? a.getResolvedBy().getId() : null)
                .resolvedAt(a.getResolvedAt())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
