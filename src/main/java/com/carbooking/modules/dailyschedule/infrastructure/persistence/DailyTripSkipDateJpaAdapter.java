package com.carbooking.modules.dailyschedule.infrastructure.persistence;

import com.carbooking.entity.DailySchedulePassenger;
import com.carbooking.entity.DailyTripSkipDate;
import com.carbooking.modules.dailyschedule.domain.port.DailyTripSkipDatePort;
import com.carbooking.repository.DailyTripSkipDateRepository;
import com.carbooking.modules.dailyschedule.domain.port.DailyTripSkipDatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DailyTripSkipDateJpaAdapter implements DailyTripSkipDatePort {

    private final DailyTripSkipDateRepository repo;

    @Override public boolean existsBySchedulePassengerAndSkipDate(DailySchedulePassenger passenger, LocalDate date) { return repo.existsBySchedulePassengerAndSkipDate(passenger, date); }
    @Override public Optional<DailyTripSkipDate> findBySchedulePassengerAndSkipDate(DailySchedulePassenger passenger, LocalDate date) { return repo.findBySchedulePassengerAndSkipDate(passenger, date); }
    @Override public List<DailyTripSkipDate> findBySchedulePassengerAndSkipDateGreaterThanEqual(DailySchedulePassenger passenger, LocalDate from) { return repo.findBySchedulePassengerAndSkipDateGreaterThanEqual(passenger, from); }
    @Override public DailyTripSkipDate save(DailyTripSkipDate skipDate) { return repo.save(skipDate); }
    @Override public void delete(DailyTripSkipDate skipDate) { repo.delete(skipDate); }
}
