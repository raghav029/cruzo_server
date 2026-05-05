package com.carbooking.repository;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.entity.Booking;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Page<Booking> findByTenant(Tenant tenant, Pageable pageable);
    Page<Booking> findByEmployee(User employee, Pageable pageable);
    Page<Booking> findByCorporateClient(CorporateClient client, Pageable pageable);
    Page<Booking> findByTenantAndStatus(Tenant tenant, BookingStatus status, Pageable pageable);
    Page<Booking> findByTenantAndScheduledAtBetween(Tenant tenant, java.time.Instant from, java.time.Instant to, Pageable pageable);
    Page<Booking> findByTenantAndStatusAndScheduledAtBetween(Tenant tenant, BookingStatus status, java.time.Instant from, java.time.Instant to, Pageable pageable);

    List<Booking> findByCorporateClientAndStatusAndTripCompletedAtBetween(
            CorporateClient client, BookingStatus status,
            java.time.Instant from, java.time.Instant to);

    @Query("SELECT b FROM Booking b WHERE b.corporateClient = :client AND b.status = 'COMPLETED' " +
           "AND b.tripCompletedAt BETWEEN :from AND :to " +
           "AND b.id NOT IN (SELECT li.booking.id FROM InvoiceLineItem li)")
    List<Booking> findUninvoicedCompletedBookings(@Param("client") CorporateClient client,
            @Param("from") java.time.Instant from, @Param("to") java.time.Instant to);

    Optional<Booking> findFirstByDriverAndStatusIn(Driver driver, List<BookingStatus> statuses);

    long countByTenant(Tenant tenant);
    long countByTenantAndStatus(Tenant tenant, BookingStatus status);
    long countByTenantAndCreatedAtBetween(Tenant tenant, java.time.Instant from, java.time.Instant to);

    @Query("SELECT COALESCE(SUM(b.finalFare), 0) FROM Booking b WHERE b.tenant = :tenant " +
           "AND b.status = :status AND b.tripCompletedAt BETWEEN :from AND :to")
    java.math.BigDecimal sumFinalFareByTenantAndStatusAndTripCompletedAtBetween(
            @Param("tenant") Tenant tenant, @Param("status") BookingStatus status,
            @Param("from") java.time.Instant from, @Param("to") java.time.Instant to);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.tenant = :tenant AND b.status NOT IN " +
           "('COMPLETED','CANCELLED_BY_EMPLOYEE','CANCELLED_BY_ADMIN','CANCELLED_BY_FLEET_MANAGER','CANCELLED_BY_DRIVER','REJECTED')")
    long countActiveByTenant(Tenant tenant);
}
