package com.carbooking.modules.dailyschedule.domain.port;

import com.carbooking.entity.DailySchedulePassenger;
import com.carbooking.entity.DailyTripSkipDate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyTripSkipDatePort {
    boolean existsBySchedulePassengerAndSkipDate(DailySchedulePassenger passenger, LocalDate date);
    Optional<DailyTripSkipDate> findBySchedulePassengerAndSkipDate(DailySchedulePassenger passenger, LocalDate date);
    List<DailyTripSkipDate> findBySchedulePassengerAndSkipDateGreaterThanEqual(DailySchedulePassenger passenger, LocalDate from);
    DailyTripSkipDate save(DailyTripSkipDate skipDate);
    void delete(DailyTripSkipDate skipDate);
}
