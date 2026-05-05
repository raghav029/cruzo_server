package com.carbooking.scheduler;
import com.carbooking.modules.notification.application.NotificationService;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.entity.Booking;
import com.carbooking.entity.RecurringBooking;
import com.carbooking.repository.BookingRepository;
import com.carbooking.repository.RecurringBookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Component
public class RecurringBookingScheduler {

    private final RecurringBookingRepository recurringBookingRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    public RecurringBookingScheduler(RecurringBookingRepository recurringBookingRepository,
                                     BookingRepository bookingRepository,
                                     @Lazy NotificationService notificationService) {
        this.recurringBookingRepository = recurringBookingRepository;
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void createNextDayBookings() {
        LocalDate tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        String dayAbbr = getDayAbbreviation(tomorrow.getDayOfWeek());

        log.info("RecurringBookingScheduler: processing bookings for {} ({})", tomorrow, dayAbbr);

        List<RecurringBooking> activeRecurring = recurringBookingRepository.findByIsActiveTrue();

        for (RecurringBooking rb : activeRecurring) {
            try {
                if (!rb.getRecurrenceDays().contains(dayAbbr)) {
                    continue;
                }

                Instant scheduledAt = tomorrow.atTime(rb.getScheduledTime()).toInstant(ZoneOffset.UTC);

                Booking booking = Booking.builder()
                        .tenant(rb.getTenant())
                        .corporateClient(rb.getCorporateClient())
                        .employee(rb.getEmployee())
                        .pickupAddress(rb.getPickupAddress())
                        .dropAddress(rb.getDropAddress())
                        .vehicleTypeRequested(rb.getVehicleType())
                        .scheduledAt(scheduledAt)
                        .notes("Auto-created from recurring booking. " + (rb.getNotes() != null ? rb.getNotes() : ""))
                        .status(BookingStatus.PENDING_APPROVAL)
                        .build();

                Booking saved = bookingRepository.save(booking);
                notificationService.notifyBookingCreated(saved);
                log.info("Created recurring booking {} for employee {} on {}", saved.getId(), rb.getEmployee().getId(), tomorrow);
            } catch (Exception e) {
                log.error("Failed to create recurring booking for id {}: {}", rb.getId(), e.getMessage());
            }
        }
    }

    private String getDayAbbreviation(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
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
