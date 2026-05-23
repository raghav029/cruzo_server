package com.carbooking.repository;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.entity.Booking;
import com.carbooking.entity.enums.BookingType;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Page<Booking> findByTenant(Tenant tenant, Pageable pageable);
    Page<Booking> findByEmployee(User employee, Pageable pageable);
    Page<Booking> findByCorporateClient(CorporateClient client, Pageable pageable);
    Page<Booking> findByTenantAndStatus(Tenant tenant, BookingStatus status, Pageable pageable);
    List<Booking> findByTenantAndStatus(Tenant tenant, BookingStatus status);
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

    Page<Booking> findByDriver(Driver driver, Pageable pageable);
    Page<Booking> findByDriverAndStatus(Driver driver, BookingStatus status, Pageable pageable);
    List<Booking> findByEmployeeAndStatusIn(User employee, List<BookingStatus> statuses);
    Optional<Booking> findFirstByDriverAndStatusIn(Driver driver, List<BookingStatus> statuses);
    long countByDriverAndStatus(Driver driver, BookingStatus status);
    long countByDriverAndStatusAndTripCompletedAtAfter(Driver driver, BookingStatus status, Instant after);

    @Query("SELECT COALESCE(SUM(b.finalFare), 0) FROM Booking b WHERE b.driver = :driver AND b.status = :status")
    java.math.BigDecimal sumFinalFareByDriverAndStatus(@Param("driver") Driver driver, @Param("status") BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.finalFare), 0) FROM Booking b WHERE b.driver = :driver AND b.status = :status AND b.tripCompletedAt > :after")
    java.math.BigDecimal sumFinalFareByDriverAndStatusAndTripCompletedAtAfter(@Param("driver") Driver driver, @Param("status") BookingStatus status, @Param("after") Instant after);

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

    @Query(value = "SELECT EXTRACT(HOUR FROM scheduled_at AT TIME ZONE :tz)::int AS hr, COUNT(*)::int AS cnt " +
                   "FROM bookings WHERE tenant_id = :tenantId AND scheduled_at >= :from AND scheduled_at < :to " +
                   "GROUP BY hr ORDER BY hr", nativeQuery = true)
    List<Object[]> countGroupedByHour(@Param("tenantId") UUID tenantId, @Param("tz") String tz,
                                      @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = "SELECT DATE(trip_completed_at AT TIME ZONE :tz) AS day, " +
                   "COALESCE(SUM(final_fare), 0)::bigint AS rev " +
                   "FROM bookings WHERE tenant_id = :tenantId AND status = 'COMPLETED' " +
                   "AND trip_completed_at >= :from AND trip_completed_at < :to " +
                   "GROUP BY day ORDER BY day", nativeQuery = true)
    List<Object[]> sumRevenueGroupedByDay(@Param("tenantId") UUID tenantId, @Param("tz") String tz,
                                          @Param("from") Instant from, @Param("to") Instant to);

    Page<Booking> findByCustomerIdAndBookingType(UUID customerId, BookingType bookingType, Pageable pageable);
    Page<Booking> findByTenantIdAndBookingType(UUID tenantId, BookingType bookingType, Pageable pageable);

    // --- Corporate Dashboard queries ---

    long countByCorporateClientAndStatus(CorporateClient client, BookingStatus status);

    long countByCorporateClientAndCreatedAtBetween(CorporateClient client, Instant from, Instant to);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.corporateClient = :client " +
           "AND b.status IN ('CANCELLED_BY_EMPLOYEE','CANCELLED_BY_ADMIN','CANCELLED_BY_FLEET_MANAGER','CANCELLED_BY_DRIVER') " +
           "AND b.createdAt BETWEEN :from AND :to")
    long countCancelledByCorporateClientAndCreatedAtBetween(
            @Param("client") CorporateClient client,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("SELECT COALESCE(SUM(CASE WHEN b.finalFare IS NOT NULL THEN b.finalFare ELSE b.estimatedFare END), 0) " +
           "FROM Booking b WHERE b.corporateClient = :client AND b.status = 'COMPLETED' " +
           "AND b.tripCompletedAt BETWEEN :from AND :to")
    BigDecimal sumSpendByCorporateClientAndTripCompletedAtBetween(
            @Param("client") CorporateClient client,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query(value = "SELECT DATE(trip_completed_at AT TIME ZONE :tz) AS day, " +
                   "COALESCE(SUM(COALESCE(final_fare, estimated_fare)), 0)::bigint AS spend " +
                   "FROM bookings WHERE corporate_client_id = :clientId AND status = 'COMPLETED' " +
                   "AND trip_completed_at >= :from AND trip_completed_at < :to " +
                   "GROUP BY day ORDER BY day", nativeQuery = true)
    List<Object[]> sumSpendGroupedByDayForClient(
            @Param("clientId") UUID clientId,
            @Param("tz") String tz,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("SELECT b FROM Booking b WHERE b.corporateClient = :client " +
           "AND b.status NOT IN ('COMPLETED','CANCELLED_BY_EMPLOYEE','CANCELLED_BY_ADMIN'," +
           "'CANCELLED_BY_FLEET_MANAGER','CANCELLED_BY_DRIVER','REJECTED') " +
           "AND b.scheduledAt > :now ORDER BY b.scheduledAt ASC")
    List<Booking> findUpcomingByCorporateClient(
            @Param("client") CorporateClient client,
            @Param("now") Instant now,
            org.springframework.data.domain.Pageable pageable);

    // --- B2C Dashboard queries ---

    @Query("SELECT COALESCE(SUM(b.finalFare), 0) FROM Booking b " +
           "WHERE b.customer.id = :customerId AND b.status = 'COMPLETED'")
    BigDecimal sumLifetimeSpendByCustomerId(@Param("customerId") UUID customerId);

    long countByCustomerIdAndStatus(UUID customerId, BookingStatus status);

    long countByCustomerIdAndCreatedAtBetween(UUID customerId, Instant from, Instant to);

    @Query("SELECT COALESCE(SUM(b.finalFare), 0) FROM Booking b " +
           "WHERE b.customer.id = :customerId AND b.status = 'COMPLETED' " +
           "AND b.tripCompletedAt BETWEEN :from AND :to")
    BigDecimal sumFinalFareByCustomerIdAndTripCompletedAtBetween(
            @Param("customerId") UUID customerId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId " +
           "AND b.status IN ('PENDING_APPROVAL','APPROVED','DRIVER_ASSIGNED','DRIVER_EN_ROUTE','ARRIVED') " +
           "AND b.scheduledAt > :now ORDER BY b.scheduledAt ASC")
    List<Booking> findUpcomingByCustomerId(
            @Param("customerId") UUID customerId,
            @Param("now") Instant now,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId " +
           "AND b.status IN ('COMPLETED','CANCELLED_BY_EMPLOYEE','CANCELLED_BY_ADMIN'," +
           "'CANCELLED_BY_FLEET_MANAGER','CANCELLED_BY_DRIVER') " +
           "ORDER BY b.tripCompletedAt DESC NULLS LAST, b.createdAt DESC")
    List<Booking> findRecentByCustomerId(
            @Param("customerId") UUID customerId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId AND b.status = 'IN_PROGRESS'")
    java.util.Optional<Booking> findActiveByCustomerId(@Param("customerId") UUID customerId);

    // --- Fleet Dashboard extra ---

    long countByTenantAndBookingType(Tenant tenant, com.carbooking.entity.enums.BookingType bookingType);

    long countByTenantAndBookingTypeAndCreatedAtBetween(
            Tenant tenant,
            com.carbooking.entity.enums.BookingType bookingType,
            Instant from, Instant to);
}
