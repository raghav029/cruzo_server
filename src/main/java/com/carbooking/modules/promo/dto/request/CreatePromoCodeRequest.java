package com.carbooking.modules.promo.dto.request;

import com.carbooking.entity.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter
public class CreatePromoCodeRequest {
    @NotBlank private String code;
    private String description;
    @NotNull private DiscountType discountType;
    @NotNull @DecimalMin("0.01") private BigDecimal discountValue;
    private BigDecimal minBookingValue;
    private BigDecimal maxDiscountAmount;
    private Integer maxUses;
    @NotNull private Instant validFrom;
    private Instant validUntil;
}
