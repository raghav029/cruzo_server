package com.carbooking.modules.fleet.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DriverStatsResponse {
    private long totalTrips;
    private long tripsThisMonth;
    private BigDecimal totalEarnings;
    private BigDecimal earningsThisMonth;
}
