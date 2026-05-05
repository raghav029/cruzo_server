package com.carbooking.modules.client.dto.response;

import com.carbooking.common.enums.BillingCycle;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CorporateClientResponse {
    private UUID id;
    private UUID tenantId;
    private String companyName;
    private String gstNumber;
    private String billingAddress;
    private String billingEmail;
    private BillingCycle billingCycle;
    private BigDecimal creditLimit;
    private BigDecimal currentOutstanding;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
