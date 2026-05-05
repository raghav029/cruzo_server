package com.carbooking.modules.dailyschedule.domain.port;

import com.carbooking.common.enums.DailyTripPassengerStatus;
import com.carbooking.entity.DailyTrip;
import com.carbooking.entity.DailyTripPassenger;
import com.carbooking.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyTripPassengerPort {
    Optional<DailyTripPassenger> findById(UUID id);
    List<DailyTripPassenger> findByDailyTripOrderByStopSequenceAsc(DailyTrip trip);
    Optional<DailyTripPassenger> findByDailyTripAndEmployee(DailyTrip trip, User employee);
    Optional<DailyTripPassenger> findByEmployeeAndDailyTrip_TripDate(User employee, LocalDate date);
    List<DailyTripPassenger> findByEmployeeAndDailyTrip_TripDateBetween(User employee, LocalDate from, LocalDate to);
    List<DailyTripPassenger> findByDailyTripAndStatusIn(DailyTrip trip, List<DailyTripPassengerStatus> statuses);
    DailyTripPassenger save(DailyTripPassenger passenger);
}
