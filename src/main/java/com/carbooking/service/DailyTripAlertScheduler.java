package com.carbooking.service;

import com.carbooking.common.enums.DailyTripStatus;
import com.carbooking.entity.DailyTrip;
import com.carbooking.repository.DailyTripRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Component
public class DailyTripAlertScheduler {

    private final DailyTripRepository dailyTripRepository;
    private final NotificationService notificationService;

    @Autowired
    public DailyTripAlertScheduler(
            DailyTripRepository dailyTripRepository,
            @Lazy NotificationService notificationService) {
        this.dailyTripRepository = dailyTripRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 7 * * *") // 7am UTC daily
    @Transactional
    public void alertUnassignedTrips() {
        // Find all trips for today with status=SCHEDULED and driverAlertSent=false
        List<DailyTrip> unassigned = dailyTripRepository
                .findByTripDateAndStatusAndDriverAlertSentFalse(LocalDate.now(ZoneOffset.UTC), DailyTripStatus.SCHEDULED);

        for (DailyTrip trip : unassigned) {
            notificationService.notifyUnassignedDailyTrip(trip);
            trip.setDriverAlertSent(true);
            dailyTripRepository.save(trip);
        }
    }
}
