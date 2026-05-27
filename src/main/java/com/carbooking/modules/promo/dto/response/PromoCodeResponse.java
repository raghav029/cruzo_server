package com.carbooking.modules.promo.dto.response;

import com.carbooking.entity.enums.DiscountType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Builder
public class PromoCodeResponse {
    private UUID id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minBookingValue;
    private BigDecimal maxDiscountAmount;
    private Integer maxUses;
    private int usedCount;
    private Instant validFrom;
    private Instant validUntil;
    private boolean active;
    private Instant createdAt;
}
