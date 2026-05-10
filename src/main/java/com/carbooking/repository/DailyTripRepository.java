package com.carbooking.repository;

import com.carbooking.common.enums.DailyTripStatus;
import com.carbooking.entity.DailySchedule;
import com.carbooking.entity.DailyTrip;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyTripRepository extends JpaRepository<DailyTrip, UUID> {
    List<DailyTrip> findByTenantAndTripDate(Tenant tenant, LocalDate date);
    Optional<DailyTrip> findByDailyScheduleAndTripDate(DailySchedule schedule, LocalDate date);
    List<DailyTrip> findByTripDateAndStatusAndDriverAlertSentFalse(LocalDate date, DailyTripStatus status);
    Optional<DailyTrip> findFirstByDriverAndTripDateAndStatusIn(Driver driver, LocalDate date, List<DailyTripStatus> statuses);
    long countByTenantAndTripDate(Tenant tenant, LocalDate date);
    long countByTenantAndTripDateAndDriverIsNull(Tenant tenant, LocalDate date);

    @Query(value = "SELECT trip_date, COUNT(*)::int AS cnt FROM daily_trips " +
                   "WHERE tenant_id = :tenantId AND trip_date >= :from AND trip_date <= :to " +
                   "GROUP BY trip_date ORDER BY trip_date", nativeQuery = true)
    List<Object[]> countGroupedByDay(@Param("tenantId") UUID tenantId,
                                     @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT trip_date, COUNT(*)::int AS cnt FROM daily_trips " +
                   "WHERE tenant_id = :tenantId AND driver_id IS NULL " +
                   "AND trip_date >= :from AND trip_date <= :to " +
                   "GROUP BY trip_date ORDER BY trip_date", nativeQuery = true)
    List<Object[]> countUnassignedGroupedByDay(@Param("tenantId") UUID tenantId,
                                               @Param("from") LocalDate from, @Param("to") LocalDate to);
}
