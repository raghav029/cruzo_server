package com.carbooking.modules.tenant.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class TenantStatsResponse {

    private UUID tenantId;
    private String tenantName;
    private long totalBookings;
    private long activeBookings;
    private long totalDrivers;
    private long availableDrivers;
    private long totalVehicles;
    private long activeVehicles;
    private long totalEmployees;
}
