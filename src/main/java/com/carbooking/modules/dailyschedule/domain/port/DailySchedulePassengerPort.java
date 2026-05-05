package com.carbooking.modules.dailyschedule.domain.port;

import com.carbooking.entity.DailySchedule;
import com.carbooking.entity.DailySchedulePassenger;
import com.carbooking.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailySchedulePassengerPort {
    Optional<DailySchedulePassenger> findById(UUID id);
    List<DailySchedulePassenger> findByDailyScheduleAndIsActiveTrue(DailySchedule schedule);
    List<DailySchedulePassenger> findByDailyScheduleAndIsActiveTrueAndStopSequenceIsNull(DailySchedule schedule);
    boolean existsByDailyScheduleAndEmployee(DailySchedule schedule, User employee);
    DailySchedulePassenger save(DailySchedulePassenger passenger);
}
