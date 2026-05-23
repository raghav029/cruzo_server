package com.carbooking.modules.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class CorporateDashboardResponse {

    private long pendingApprovals;
    private long activeBookings;
    private long totalBookingsThisMonth;
    private long cancelledThisMonth;

    private BigDecimal spendThisMonth;
    private BigDecimal spendLastMonth;
    private List<Integer> spendSparkData;

    private long totalEmployees;
    private long employeesWithActiveTrip;

    private long tripsToday;
    private long passengersToday;

    private List<UpcomingBookingItem> upcomingBookings;

    @Getter
    @AllArgsConstructor
    public static class UpcomingBookingItem {
        private UUID bookingId;
        private String employeeName;
        private Instant scheduledAt;
        private String pickupAddress;
        private String status;
    }
}
