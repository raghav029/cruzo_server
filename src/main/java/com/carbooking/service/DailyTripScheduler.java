package com.carbooking.service;

import com.carbooking.common.enums.DailyTripPassengerStatus;
import com.carbooking.common.enums.DailyTripStatus;
import com.carbooking.entity.*;
import com.carbooking.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
public class DailyTripScheduler {

    private final DailyScheduleRepository dailyScheduleRepository;
    private final DailySchedulePassengerRepository dailySchedulePassengerRepository;
    private final DailyTripRepository dailyTripRepository;
    private final DailyTripPassengerRepository dailyTripPassengerRepository;
    private final DailyTripSkipDateRepository dailyTripSkipDateRepository;
    private final NotificationService notificationService;

    @Autowired
    public DailyTripScheduler(
            DailyScheduleRepository dailyScheduleRepository,
            DailySchedulePassengerRepository dailySchedulePassengerRepository,
            DailyTripRepository dailyTripRepository,
            DailyTripPassengerRepository dailyTripPassengerRepository,
            DailyTripSkipDateRepository dailyTripSkipDateRepository,
            @Lazy NotificationService notificationService) {
        this.dailyScheduleRepository = dailyScheduleRepository;
        this.dailySchedulePassengerRepository = dailySchedulePassengerRepository;
        this.dailyTripRepository = dailyTripRepository;
        this.dailyTripPassengerRepository = dailyTripPassengerRepository;
        this.dailyTripSkipDateRepository = dailyTripSkipDateRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 0 * * *") // midnight UTC
    @Transactional
    public void createDailyTrips() {
        List<DailySchedule> activeSchedules = dailyScheduleRepository.findByIsActiveTrue();

        for (DailySchedule schedule : activeSchedules) {
            try {
                // Get tenant timezone
                ZoneId zoneId = ZoneId.of(schedule.getTenant().getTimezone());
                LocalDate tomorrow = LocalDate.now(zoneId).plusDays(1);

                // Check if tomorrow matches recurrence days
                String dayAbbr = getDayAbbr(tomorrow.getDayOfWeek());
                if (!schedule.getRecurrenceDays().contains(dayAbbr)) continue;

                // Check no duplicate trip
                if (dailyTripRepository.findByDailyScheduleAndTripDate(schedule, tomorrow).isPresent()) continue;

                // Create trip
                DailyTrip trip = DailyTrip.builder()
                        .tenant(schedule.getTenant())
                        .dailySchedule(schedule)
                        .tripDate(tomorrow)
                        .scheduledPickupTime(schedule.getPickupTime())
                        .dropAddress(schedule.getDropAddress())
                        .status(DailyTripStatus.SCHEDULED)
                        .build();
                trip = dailyTripRepository.save(trip);

                // Create passenger rows
                List<DailySchedulePassenger> passengers = dailySchedulePassengerRepository
                        .findByDailyScheduleAndIsActiveTrue(schedule);

                int createdCount = 0;
                for (DailySchedulePassenger p : passengers) {
                    // Check skip
                    if (dailyTripSkipDateRepository.existsBySchedulePassengerAndSkipDate(p, tomorrow)) {
                        log.info("Skipping passenger {} for date {}", p.getId(), tomorrow);
                        continue;
                    }

                    String boardingOtp = generateOtp();
                    String dropOtp = generateOtp();
                    // ensure they're different
                    while (dropOtp.equals(boardingOtp)) dropOtp = generateOtp();

                    DailyTripPassenger passenger = DailyTripPassenger.builder()
                            .tenant(schedule.getTenant())
                            .dailyTrip(trip)
                            .employee(p.getEmployee())
                            .pickupAddress(p.getPickupAddress())
                            .pickupLat(p.getPickupLat())
                            .pickupLng(p.getPickupLng())
                            .stopSequence(p.getStopSequence())
                            .boardingOtp(boardingOtp)
                            .dropOtp(dropOtp)
                            .status(DailyTripPassengerStatus.SCHEDULED)
                            .build();
                    dailyTripPassengerRepository.save(passenger);

                    // SMS employee
                    notificationService.notifyDailyTripScheduled(passenger, trip);
                    createdCount++;
                }

                // If no passengers created (all skipped), cancel the trip
                if (createdCount == 0) {
                    trip.setStatus(DailyTripStatus.CANCELLED);
                    dailyTripRepository.save(trip);
                    log.info("All passengers skipped for trip {} on {}, auto-cancelled", trip.getId(), tomorrow);
                }

            } catch (Exception e) {
                log.error("Failed creating daily trip for schedule {}: {}", schedule.getId(), e.getMessage());
            }
        }
    }

    private String generateOtp() {
        return String.format("%04d", new java.util.Random().nextInt(10000));
    }

    private String getDayAbbr(DayOfWeek d) {
        return switch (d) {
            case MONDAY -> "MON";
            case TUESDAY -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY -> "THU";
            case FRIDAY -> "FRI";
            case SATURDAY -> "SAT";
            case SUNDAY -> "SUN";
        };
    }
}
