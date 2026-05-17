package com.carbooking.modules.client.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TravelPolicyResponse {
    private BigDecimal maxBookingValue;
    private String allowedVehicleTypes;
}
