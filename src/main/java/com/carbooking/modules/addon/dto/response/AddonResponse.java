package com.carbooking.modules.addon.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter @Builder
public class AddonResponse {
    private UUID id;
    private String name;
    private BigDecimal price;
    private boolean active;
    private Instant createdAt;
}
