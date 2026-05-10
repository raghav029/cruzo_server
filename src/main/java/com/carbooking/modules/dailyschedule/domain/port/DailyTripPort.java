package com.carbooking.modules.dailyschedule.domain.port;

import com.carbooking.common.enums.DailyTripStatus;
import com.carbooking.entity.DailySchedule;
import com.carbooking.entity.DailyTrip;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyTripPort {
    Optional<DailyTrip> findById(UUID id);
    List<DailyTrip> findByTenantAndTripDate(Tenant tenant, LocalDate date);
    Optional<DailyTrip> findByDailyScheduleAndTripDate(DailySchedule schedule, LocalDate date);
    Optional<DailyTrip> findFirstByDriverAndTripDateAndStatusIn(Driver driver, LocalDate date, List<DailyTripStatus> statuses);
    long countByTenantAndTripDate(Tenant tenant, LocalDate date);
    long countByTenantAndTripDateAndDriverIsNull(Tenant tenant, LocalDate date);
    DailyTrip save(DailyTrip trip);
    List<Object[]> countGroupedByDay(UUID tenantId, LocalDate from, LocalDate to);
    List<Object[]> countUnassignedGroupedByDay(UUID tenantId, LocalDate from, LocalDate to);
}
