package com.carbooking.modules.promo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter @Builder
public class ValidatePromoResponse {
    private String code;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
}
