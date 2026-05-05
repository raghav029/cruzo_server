package com.carbooking.modules.dailyschedule.infrastructure.persistence;

import com.carbooking.entity.DailySchedule;
import com.carbooking.entity.DailySchedulePassenger;
import com.carbooking.entity.User;
import com.carbooking.modules.dailyschedule.domain.port.DailySchedulePassengerPort;
import com.carbooking.repository.DailySchedulePassengerRepository;
import com.carbooking.modules.dailyschedule.domain.port.DailySchedulePassengerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DailySchedulePassengerJpaAdapter implements DailySchedulePassengerPort {

    private final DailySchedulePassengerRepository repo;

    @Override public Optional<DailySchedulePassenger> findById(UUID id) { return repo.findById(id); }
    @Override public List<DailySchedulePassenger> findByDailyScheduleAndIsActiveTrue(DailySchedule schedule) { return repo.findByDailyScheduleAndIsActiveTrue(schedule); }
    @Override public List<DailySchedulePassenger> findByDailyScheduleAndIsActiveTrueAndStopSequenceIsNull(DailySchedule schedule) { return repo.findByDailyScheduleAndIsActiveTrueAndStopSequenceIsNull(schedule); }
    @Override public boolean existsByDailyScheduleAndEmployee(DailySchedule schedule, User employee) { return repo.existsByDailyScheduleAndEmployee(schedule, employee); }
    @Override public DailySchedulePassenger save(DailySchedulePassenger passenger) { return repo.save(passenger); }
}
