package com.carbooking.repository;

import com.carbooking.common.enums.DailyTripPassengerStatus;
import com.carbooking.entity.DailyTrip;
import com.carbooking.entity.DailyTripPassenger;
import com.carbooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyTripPassengerRepository extends JpaRepository<DailyTripPassenger, UUID> {
    List<DailyTripPassenger> findByDailyTripOrderByStopSequenceAsc(DailyTrip trip);
    List<DailyTripPassenger> findByDailyTripAndStatusIn(DailyTrip trip, List<DailyTripPassengerStatus> statuses);
    Optional<DailyTripPassenger> findByDailyTripAndEmployee(DailyTrip trip, User employee);
    List<DailyTripPassenger> findByEmployeeAndDailyTrip_TripDateBetween(User employee, LocalDate from, LocalDate to);
    Optional<DailyTripPassenger> findByEmployeeAndDailyTrip_TripDate(User employee, LocalDate date);
}
