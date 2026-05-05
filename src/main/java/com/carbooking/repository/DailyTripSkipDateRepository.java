package com.carbooking.repository;

import com.carbooking.entity.DailySchedulePassenger;
import com.carbooking.entity.DailyTripSkipDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyTripSkipDateRepository extends JpaRepository<DailyTripSkipDate, UUID> {
    boolean existsBySchedulePassengerAndSkipDate(DailySchedulePassenger passenger, LocalDate date);
    Optional<DailyTripSkipDate> findBySchedulePassengerAndSkipDate(DailySchedulePassenger passenger, LocalDate date);
    List<DailyTripSkipDate> findBySchedulePassengerAndSkipDateGreaterThanEqual(DailySchedulePassenger passenger, LocalDate from);
}
