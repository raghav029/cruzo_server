package com.carbooking.modules.report.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Builder
public class OverviewStatsResponse {
    private BigDecimal revenueMtd;
    private long bookingsMtd;
    private long activeTrips;
    private long availableCars;
    private long pendingApprovals;
    private double fleetOccupancyPct;
    private List<TopVehicleEntry> topVehicles;
    private List<TopClientEntry> topCorporateClients;

    @Getter @Builder
    public static class TopVehicleEntry {
        private String vehicleName;
        private long tripCount;
    }

    @Getter @Builder
    public static class TopClientEntry {
        private String clientName;
        private BigDecimal revenue;
    }
}
