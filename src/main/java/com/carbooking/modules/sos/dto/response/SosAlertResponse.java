package com.carbooking.modules.sos.dto.response;

import com.carbooking.common.enums.SosStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class SosAlertResponse {
    private UUID id;
    private UUID tenantId;
    private UUID bookingId;
    private UUID triggeredByUserId;
    private String triggeredByName;
    private BigDecimal lat;
    private BigDecimal lng;
    private String message;
    private SosStatus status;
    private UUID resolvedByUserId;
    private Instant resolvedAt;
    private Instant createdAt;
}
