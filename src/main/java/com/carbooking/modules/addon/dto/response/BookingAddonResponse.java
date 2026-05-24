package com.carbooking.modules.addon.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Builder
public class BookingAddonResponse {
    private UUID addonId;
    private String name;
    private int quantity;
    private BigDecimal priceSnapshot;
    private BigDecimal total;
}
