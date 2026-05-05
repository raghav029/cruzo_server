package com.carbooking.dto.response.report;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class FleetSummaryResponse {
    private long totalBookings;
    private long completedBookings;
    private long cancelledBookings;
    private BigDecimal totalRevenue;
    private BigDecimal averageFare;
    private List<DriverStat> topDrivers;
    private List<VehicleUtilization> vehicleUtilization;

    @Getter
    @Builder
    public static class DriverStat {
        private String driverName;
        private long completedTrips;
    }

    @Getter
    @Builder
    public static class VehicleUtilization {
        private String plateNumber;
        private String vehicleType;
        private long trips;
    }
}
