package com.carbooking.modules.booking.infrastructure.persistence;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.entity.Booking;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookingJpaAdapter implements BookingPort {

    private final BookingRepository repo;

    @Override public Optional<Booking> findById(UUID id) { return repo.findById(id); }
    @Override public Page<Booking> findByTenant(Tenant tenant, Pageable pageable) { return repo.findByTenant(tenant, pageable); }
    @Override public Page<Booking> findByEmployee(User employee, Pageable pageable) { return repo.findByEmployee(employee, pageable); }
    @Override public Page<Booking> findByCorporateClient(CorporateClient client, Pageable pageable) { return repo.findByCorporateClient(client, pageable); }
    @Override public Page<Booking> findByTenantAndStatus(Tenant tenant, BookingStatus status, Pageable pageable) { return repo.findByTenantAndStatus(tenant, status, pageable); }
    @Override public Page<Booking> findByTenantAndScheduledAtBetween(Tenant tenant, Instant from, Instant to, Pageable pageable) { return repo.findByTenantAndScheduledAtBetween(tenant, from, to, pageable); }
    @Override public Page<Booking> findByTenantAndStatusAndScheduledAtBetween(Tenant tenant, BookingStatus status, Instant from, Instant to, Pageable pageable) { return repo.findByTenantAndStatusAndScheduledAtBetween(tenant, status, from, to, pageable); }
    @Override public Page<Booking> findByDriver(Driver driver, Pageable pageable) { return repo.findByDriver(driver, pageable); }
    @Override public Page<Booking> findByDriverAndStatus(Driver driver, BookingStatus status, Pageable pageable) { return repo.findByDriverAndStatus(driver, status, pageable); }
    @Override public List<Booking> findByEmployeeAndStatusIn(User employee, List<BookingStatus> statuses) { return repo.findByEmployeeAndStatusIn(employee, statuses); }
    @Override public Optional<Booking> findFirstByDriverAndStatusIn(Driver driver, List<BookingStatus> statuses) { return repo.findFirstByDriverAndStatusIn(driver, statuses); }
    @Override public long countByDriverAndStatus(Driver driver, BookingStatus status) { return repo.countByDriverAndStatus(driver, status); }
    @Override public long countByDriverAndStatusAndTripCompletedAtAfter(Driver driver, BookingStatus status, java.time.Instant after) { return repo.countByDriverAndStatusAndTripCompletedAtAfter(driver, status, after); }
    @Override public java.math.BigDecimal sumFinalFareByDriverAndStatus(Driver driver, BookingStatus status) { return repo.sumFinalFareByDriverAndStatus(driver, status); }
    @Override public java.math.BigDecimal sumFinalFareByDriverAndStatusAndTripCompletedAtAfter(Driver driver, BookingStatus status, java.time.Instant after) { return repo.sumFinalFareByDriverAndStatusAndTripCompletedAtAfter(driver, status, after); }
    @Override public List<Booking> findUninvoicedCompletedBookings(CorporateClient client, Instant from, Instant to) { return repo.findUninvoicedCompletedBookings(client, from, to); }
    @Override public long countByTenant(Tenant tenant) { return repo.countByTenant(tenant); }
    @Override public long countByTenantAndStatus(Tenant tenant, BookingStatus status) { return repo.countByTenantAndStatus(tenant, status); }
    @Override public long countByTenantAndCreatedAtBetween(Tenant tenant, Instant from, Instant to) { return repo.countByTenantAndCreatedAtBetween(tenant, from, to); }
    @Override public BigDecimal sumFinalFareByTenantAndStatusAndTripCompletedAtBetween(Tenant tenant, BookingStatus status, Instant from, Instant to) { return repo.sumFinalFareByTenantAndStatusAndTripCompletedAtBetween(tenant, status, from, to); }
    @Override public long countActiveByTenant(Tenant tenant) { return repo.countActiveByTenant(tenant); }
    @Override public Booking save(Booking booking) { return repo.save(booking); }
    @Override public List<Object[]> countGroupedByHour(UUID tenantId, String tz, Instant from, Instant to) { return repo.countGroupedByHour(tenantId, tz, from, to); }
    @Override public List<Object[]> sumRevenueGroupedByDay(UUID tenantId, String tz, Instant from, Instant to) { return repo.sumRevenueGroupedByDay(tenantId, tz, from, to); }
    @Override public long countByTenantAndBookingType(Tenant tenant, com.carbooking.entity.enums.BookingType bookingType) { return repo.countByTenantAndBookingType(tenant, bookingType); }
    @Override public long countByTenantAndBookingTypeAndCreatedAtBetween(Tenant tenant, com.carbooking.entity.enums.BookingType bookingType, Instant from, Instant to) { return repo.countByTenantAndBookingTypeAndCreatedAtBetween(tenant, bookingType, from, to); }
    @Override public Optional<Booking> findActiveByCustomerId(UUID customerId) { return repo.findActiveByCustomerId(customerId); }
}
