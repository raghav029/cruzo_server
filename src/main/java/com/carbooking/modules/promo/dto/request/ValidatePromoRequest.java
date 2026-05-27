package com.carbooking.modules.promo.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ValidatePromoRequest {
    @NotBlank private String code;
    @NotNull @DecimalMin("0.01") private BigDecimal bookingAmount;
}
