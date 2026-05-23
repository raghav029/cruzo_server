package com.carbooking.modules.booking.domain.port;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.entity.Booking;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingPort {
    Optional<Booking> findById(UUID id);
    Page<Booking> findByTenant(Tenant tenant, Pageable pageable);
    Page<Booking> findByEmployee(User employee, Pageable pageable);
    Page<Booking> findByCorporateClient(CorporateClient client, Pageable pageable);
    Page<Booking> findByTenantAndStatus(Tenant tenant, BookingStatus status, Pageable pageable);
    List<Booking> findByTenantAndStatus(Tenant tenant, BookingStatus status);
    Page<Booking> findByTenantAndScheduledAtBetween(Tenant tenant, Instant from, Instant to, Pageable pageable);
    Page<Booking> findByTenantAndStatusAndScheduledAtBetween(Tenant tenant, BookingStatus status, Instant from, Instant to, Pageable pageable);
    Page<Booking> findByDriver(Driver driver, Pageable pageable);
    Page<Booking> findByDriverAndStatus(Driver driver, BookingStatus status, Pageable pageable);
    Optional<Booking> findFirstByDriverAndStatusIn(Driver driver, List<BookingStatus> statuses);
    long countByDriverAndStatus(Driver driver, BookingStatus status);
    long countByDriverAndStatusAndTripCompletedAtAfter(Driver driver, BookingStatus status, Instant after);
    BigDecimal sumFinalFareByDriverAndStatus(Driver driver, BookingStatus status);
    BigDecimal sumFinalFareByDriverAndStatusAndTripCompletedAtAfter(Driver driver, BookingStatus status, Instant after);
    List<Booking> findByEmployeeAndStatusIn(User employee, List<BookingStatus> statuses);
    List<Booking> findUninvoicedCompletedBookings(CorporateClient client, Instant from, Instant to);
    long countByTenant(Tenant tenant);
    long countByTenantAndStatus(Tenant tenant, BookingStatus status);
    long countByTenantAndCreatedAtBetween(Tenant tenant, Instant from, Instant to);
    BigDecimal sumFinalFareByTenantAndStatusAndTripCompletedAtBetween(Tenant tenant, BookingStatus status, Instant from, Instant to);
    long countActiveByTenant(Tenant tenant);
    Booking save(Booking booking);
    List<Object[]> countGroupedByHour(UUID tenantId, String tz, Instant from, Instant to);
    List<Object[]> sumRevenueGroupedByDay(UUID tenantId, String tz, Instant from, Instant to);

    // Corporate Dashboard
    long countByCorporateClientAndStatus(CorporateClient client, BookingStatus status);
    long countByCorporateClientAndCreatedAtBetween(CorporateClient client, Instant from, Instant to);
    long countCancelledByCorporateClientAndCreatedAtBetween(CorporateClient client, Instant from, Instant to);
    BigDecimal sumSpendByCorporateClientAndTripCompletedAtBetween(CorporateClient client, Instant from, Instant to);
    List<Object[]> sumSpendGroupedByDayForClient(UUID clientId, String tz, Instant from, Instant to);
    List<Booking> findUpcomingByCorporateClient(CorporateClient client, Instant now, org.springframework.data.domain.Pageable pageable);

    // B2C Dashboard
    BigDecimal sumLifetimeSpendByCustomerId(UUID customerId);
    long countByCustomerIdAndStatus(UUID customerId, BookingStatus status);
    long countByCustomerIdAndCreatedAtBetween(UUID customerId, Instant from, Instant to);
    BigDecimal sumFinalFareByCustomerIdAndTripCompletedAtBetween(UUID customerId, Instant from, Instant to);
    List<Booking> findUpcomingByCustomerId(UUID customerId, Instant now, org.springframework.data.domain.Pageable pageable);
    List<Booking> findRecentByCustomerId(UUID customerId, org.springframework.data.domain.Pageable pageable);
    java.util.Optional<Booking> findActiveByCustomerId(UUID customerId);

    // Fleet Dashboard
    long countByTenantAndBookingType(Tenant tenant, com.carbooking.entity.enums.BookingType bookingType);
    long countByTenantAndBookingTypeAndCreatedAtBetween(Tenant tenant, com.carbooking.entity.enums.BookingType bookingType, Instant from, Instant to);
}
