package com.carbooking.dto.response.dashboard;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

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
}
