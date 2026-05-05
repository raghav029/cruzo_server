package com.carbooking.modules.client.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CorporateAdminResponse {
    private UUID userId;
    private UUID tenantId;
    private UUID corporateClientId;
    private String fullName;
    private String email;
    private String phone;
    private Instant createdAt;
}
