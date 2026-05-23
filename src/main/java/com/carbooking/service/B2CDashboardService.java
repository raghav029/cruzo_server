package com.carbooking.service;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.response.b2c.B2CDashboardResponse;
import com.carbooking.dto.response.b2c.B2CDashboardResponse.ActiveTripSnapshot;
import com.carbooking.dto.response.b2c.B2CDashboardResponse.BookingSnapshot;
import com.carbooking.entity.Booking;
import com.carbooking.entity.Customer;
import com.carbooking.repository.BookingRepository;
import com.carbooking.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class B2CDashboardService {

    private final CustomerRepository customerRepo;
    private final BookingRepository bookingRepo;

    private static final BigDecimal GOLD_THRESHOLD  = new BigDecimal("100000");
    private static final BigDecimal BLACK_THRESHOLD = new BigDecimal("500000");

    @Transactional(readOnly = true)
    public B2CDashboardResponse getDashboard() {
        UUID customerId = SecurityUtils.getCurrentCustomerId();
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        String tz = customer.getTenant().getTimezone() != null
                ? customer.getTenant().getTimezone() : "Asia/Kolkata";
        ZoneId zone = ZoneId.of(tz);
        LocalDate today = LocalDate.now(zone);
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Instant endOfDay     = today.plusDays(1).atStartOfDay(zone).toInstant();

        BigDecimal lifetimeSpend = nvl(bookingRepo.sumLifetimeSpendByCustomerId(customerId));
        long lifetimeTripCount   = bookingRepo.countByCustomerIdAndStatus(customerId, BookingStatus.COMPLETED);
        String tier = computeTier(lifetimeSpend);

        ActiveTripSnapshot activeBooking = bookingRepo.findActiveByCustomerId(customerId)
                .map(this::toActiveTripSnapshot)
                .orElse(null);

        List<BookingSnapshot> upcomingBookings = bookingRepo
                .findUpcomingByCustomerId(customerId, Instant.now(), PageRequest.of(0, 3))
                .stream().map(this::toBookingSnapshot).collect(Collectors.toList());

        List<BookingSnapshot> recentBookings = bookingRepo
                .findRecentByCustomerId(customerId, PageRequest.of(0, 5))
                .stream().map(this::toBookingSnapshot).collect(Collectors.toList());

        long bookingsThisMonth = bookingRepo.countByCustomerIdAndCreatedAtBetween(
                customerId, startOfMonth, endOfDay);
        BigDecimal spendThisMonth = nvl(bookingRepo.sumFinalFareByCustomerIdAndTripCompletedAtBetween(
                customerId, startOfMonth, endOfDay));

        return B2CDashboardResponse.builder()
                .customerName(customer.getName())
                .phone(customer.getPhone())
                .tier(tier)
                .lifetimeSpend(lifetimeSpend)
                .lifetimeTripCount(lifetimeTripCount)
                .activeBooking(activeBooking)
                .upcomingBookings(upcomingBookings)
                .recentBookings(recentBookings)
                .bookingsThisMonth(bookingsThisMonth)
                .spendThisMonth(spendThisMonth)
                .build();
    }

    private String computeTier(BigDecimal lifetimeSpend) {
        if (lifetimeSpend.compareTo(BLACK_THRESHOLD) >= 0) return "BLACK";
        if (lifetimeSpend.compareTo(GOLD_THRESHOLD)  >= 0) return "GOLD";
        return "SILVER";
    }

    private ActiveTripSnapshot toActiveTripSnapshot(Booking b) {
        return new ActiveTripSnapshot(
                b.getId(),
                b.getStatus().name(),
                b.getDriver() != null ? b.getDriver().getUser().getFullName() : null,
                b.getDriver() != null ? b.getDriver().getUser().getPhone() : null,
                b.getVehicle() != null ? b.getVehicle().getPlateNumber() : null,
                b.getVehicle() != null ? b.getVehicle().getMake() + " " + b.getVehicle().getModel() : null,
                b.getScheduledAt(),
                b.getPickupAddress(),
                b.getDropAddress()
        );
    }

    private BookingSnapshot toBookingSnapshot(Booking b) {
        return new BookingSnapshot(
                b.getId(),
                b.getScheduledAt(),
                b.getStatus().name(),
                b.getVehicle() != null ? b.getVehicle().getMake() + " " + b.getVehicle().getModel() : null,
                b.getEstimatedFare(),
                b.getFinalFare()
        );
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
