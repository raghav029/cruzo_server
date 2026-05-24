package com.carbooking.modules.addon.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class UpdateAddonRequest {
    private String name;
    private BigDecimal price;
    private Boolean active;
}
