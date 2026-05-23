package com.carbooking.modules.tenant.dto.response;

import com.carbooking.entity.enums.BookingMode;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TenantResponse {

    private UUID id;
    private String name;
    private String subdomain;
    private String supportEmail;
    private String supportPhone;
    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;
    private BookingMode bookingMode;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
