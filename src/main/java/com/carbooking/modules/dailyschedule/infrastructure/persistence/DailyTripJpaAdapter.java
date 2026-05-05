package com.carbooking.modules.dailyschedule.infrastructure.persistence;

import com.carbooking.common.enums.DailyTripStatus;
import com.carbooking.entity.DailySchedule;
import com.carbooking.entity.DailyTrip;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.dailyschedule.domain.port.DailyTripPort;
import com.carbooking.repository.DailyTripRepository;
import com.carbooking.modules.dailyschedule.domain.port.DailyTripPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DailyTripJpaAdapter implements DailyTripPort {

    private final DailyTripRepository repo;

    @Override public Optional<DailyTrip> findById(UUID id) { return repo.findById(id); }
    @Override public List<DailyTrip> findByTenantAndTripDate(Tenant tenant, LocalDate date) { return repo.findByTenantAndTripDate(tenant, date); }
    @Override public Optional<DailyTrip> findByDailyScheduleAndTripDate(DailySchedule schedule, LocalDate date) { return repo.findByDailyScheduleAndTripDate(schedule, date); }
    @Override public Optional<DailyTrip> findFirstByDriverAndTripDateAndStatusIn(Driver driver, LocalDate date, List<DailyTripStatus> statuses) { return repo.findFirstByDriverAndTripDateAndStatusIn(driver, date, statuses); }
    @Override public long countByTenantAndTripDate(Tenant tenant, LocalDate date) { return repo.countByTenantAndTripDate(tenant, date); }
    @Override public long countByTenantAndTripDateAndDriverIsNull(Tenant tenant, LocalDate date) { return repo.countByTenantAndTripDateAndDriverIsNull(tenant, date); }
    @Override public DailyTrip save(DailyTrip trip) { return repo.save(trip); }
}
