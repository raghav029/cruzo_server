package com.carbooking.dto.response.b2c;

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
public class B2CDashboardResponse {

    private String customerName;
    private String phone;
    private String tier;
    private BigDecimal lifetimeSpend;
    private long lifetimeTripCount;

    private ActiveTripSnapshot activeBooking;

    private List<BookingSnapshot> upcomingBookings;
    private List<BookingSnapshot> recentBookings;

    private long bookingsThisMonth;
    private BigDecimal spendThisMonth;

    @Getter
    @AllArgsConstructor
    public static class ActiveTripSnapshot {
        private UUID bookingId;
        private String status;
        private String driverName;
        private String driverPhone;
        private String vehiclePlate;
        private String vehicleName;
        private Instant scheduledAt;
        private String pickupAddress;
        private String dropAddress;
    }

    @Getter
    @AllArgsConstructor
    public static class BookingSnapshot {
        private UUID bookingId;
        private Instant scheduledAt;
        private String status;
        private String vehicleName;
        private BigDecimal estimatedFare;
        private BigDecimal finalFare;
    }
}
