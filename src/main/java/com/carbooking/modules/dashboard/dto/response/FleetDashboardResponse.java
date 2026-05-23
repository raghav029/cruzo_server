package com.carbooking.modules.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class FleetDashboardResponse {

    private BigDecimal revenueThisMonth;
    private BigDecimal revenueLastMonth;
    private List<Integer> revenueSparkData;

    private long totalBookingsThisMonth;
    private long b2cBookingsThisMonth;
    private long corporateBookingsThisMonth;
    private long pendingApprovals;
    private long activeTrips;

    private long totalVehicles;
    private long vehiclesInTrip;
    private long vehiclesIdle;
    private double fleetOccupancy;

    private long totalDrivers;
    private long availableDrivers;

    private long tripsToday;
    private long unassignedTripsToday;

    private long activeSosAlerts;
    private long pendingInvoices;
    private long expiringDocuments;

    private long totalCustomers;
    private long newCustomersThisMonth;

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
