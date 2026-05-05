package com.carbooking.repository;

import com.carbooking.common.enums.DailyTripStatus;
import com.carbooking.entity.DailySchedule;
import com.carbooking.entity.DailyTrip;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
