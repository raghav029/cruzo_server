package com.carbooking.modules.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DashboardSummaryResponse {
    private long tripsToday;
    private long activeTrips;
    private long pendingApprovals;
    private long unassignedTrips;
    private long totalVehicles;
    private long vehiclesInTrip;
    private long totalDrivers;
    private long availableDrivers;
    private long pendingInvoices;
    private long activeSosAlerts;
    private long expiringDocuments;
    private long totalBookingsThisMonth;
    private BigDecimal revenueThisMonth;
    private List<Integer> tripsSparkData;
    private List<Integer> revenueSparkData;
    private List<Integer> unassignedSparkData;
    private List<HourlyCount> tripsByHourToday;
    private List<HourlyCount> tripsByHour7d;
    private List<HourlyCount> tripsByHour30d;

    @Getter
    @AllArgsConstructor
    public static class HourlyCount {
        private String label;
        private int value;
    }
}
