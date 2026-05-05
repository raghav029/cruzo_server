package com.carbooking.modules.config.dto.response;

import com.carbooking.common.enums.CancellationFeeType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CancellationConfigResponse {
    private UUID id;
    private UUID tenantId;
    private BigDecimal cancellationWindowHours;
    private CancellationFeeType feeType;
    private BigDecimal feeValue;
    private boolean afterWindowAllowed;
    private Instant updatedAt;
}
