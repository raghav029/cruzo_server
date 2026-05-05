package com.carbooking.dto.request.config;

import com.carbooking.common.enums.CancellationFeeType;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateCancellationConfigRequest {

    @DecimalMin(value = "0.0")
    private BigDecimal cancellationWindowHours;

    private CancellationFeeType feeType;

    @DecimalMin(value = "0.0")
    private BigDecimal feeValue;

    private Boolean afterWindowAllowed;
}
