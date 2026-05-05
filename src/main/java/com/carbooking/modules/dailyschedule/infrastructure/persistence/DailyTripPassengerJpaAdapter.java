package com.carbooking.modules.dailyschedule.infrastructure.persistence;

import com.carbooking.entity.DailyTrip;
import com.carbooking.entity.DailyTripPassenger;
import com.carbooking.entity.User;
import com.carbooking.common.enums.DailyTripPassengerStatus;
import com.carbooking.modules.dailyschedule.domain.port.DailyTripPassengerPort;
import com.carbooking.repository.DailyTripPassengerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DailyTripPassengerJpaAdapter implements DailyTripPassengerPort {

    private final DailyTripPassengerRepository repo;

    @Override public Optional<DailyTripPassenger> findById(UUID id) { return repo.findById(id); }
    @Override public List<DailyTripPassenger> findByDailyTripOrderByStopSequenceAsc(DailyTrip trip) { return repo.findByDailyTripOrderByStopSequenceAsc(trip); }
    @Override public Optional<DailyTripPassenger> findByDailyTripAndEmployee(DailyTrip trip, User employee) { return repo.findByDailyTripAndEmployee(trip, employee); }
    @Override public Optional<DailyTripPassenger> findByEmployeeAndDailyTrip_TripDate(User employee, LocalDate date) { return repo.findByEmployeeAndDailyTrip_TripDate(employee, date); }
    @Override public List<DailyTripPassenger> findByEmployeeAndDailyTrip_TripDateBetween(User employee, LocalDate from, LocalDate to) { return repo.findByEmployeeAndDailyTrip_TripDateBetween(employee, from, to); }
    @Override public List<DailyTripPassenger> findByDailyTripAndStatusIn(DailyTrip trip, List<DailyTripPassengerStatus> statuses) { return repo.findByDailyTripAndStatusIn(trip, statuses); }
    @Override public DailyTripPassenger save(DailyTripPassenger passenger) { return repo.save(passenger); }
}
