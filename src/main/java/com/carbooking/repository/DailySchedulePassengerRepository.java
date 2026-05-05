package com.carbooking.repository;

import com.carbooking.entity.DailySchedule;
import com.carbooking.entity.DailySchedulePassenger;
import com.carbooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailySchedulePassengerRepository extends JpaRepository<DailySchedulePassenger, UUID> {
    List<DailySchedulePassenger> findByDailyScheduleAndIsActiveTrue(DailySchedule schedule);
    List<DailySchedulePassenger> findByDailyScheduleAndIsActiveTrueAndStopSequenceIsNull(DailySchedule schedule);
    boolean existsByDailyScheduleAndEmployee(DailySchedule schedule, User employee);
    Optional<DailySchedulePassenger> findByDailyScheduleAndEmployee(DailySchedule schedule, User employee);
}
