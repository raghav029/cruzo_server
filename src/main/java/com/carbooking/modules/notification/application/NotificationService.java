package com.carbooking.modules.notification.application;

import com.carbooking.common.enums.NotificationChannel;
import com.carbooking.common.enums.Role;
import com.carbooking.entity.Booking;
import com.carbooking.entity.DailyTrip;
import com.carbooking.entity.DailyTripPassenger;
import com.carbooking.entity.NotificationLog;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.repository.NotificationLogRepository;
import com.carbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final RestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.msg91.auth-key:}")
    private String msg91AuthKey;

    @Value("${app.msg91.sender-id:CARBKG}")
    private String senderId;

    @Value("${app.msg91.base-url:https://api.msg91.com/api/v5}")
    private String baseUrl;

    // ── Public API ─────────────────────────────────────────────────────────

    public void notifyBookingCreated(Booking booking) {
        String msg = String.format("Your ride request has been submitted. Pickup: %s. Scheduled: %s. Ref: %s",
                booking.getPickupAddress(), booking.getScheduledAt(), shortId(booking));
        sendSms(booking, booking.getEmployee(), "BOOKING_CREATED", msg);
    }

    public void notifyBookingApproved(Booking booking) {
        String msg = String.format("Your ride on %s has been approved. Pickup: %s. Ref: %s",
                booking.getScheduledAt(), booking.getPickupAddress(), shortId(booking));
        sendSms(booking, booking.getEmployee(), "BOOKING_APPROVED", msg);
    }

    public void notifyBookingRejected(Booking booking) {
        String msg = String.format("Your ride request has been rejected. Reason: %s. Ref: %s",
                booking.getRejectionReason() != null ? booking.getRejectionReason() : "N/A", shortId(booking));
        sendSms(booking, booking.getEmployee(), "BOOKING_REJECTED", msg);
    }

    public void notifyDriverAssigned(Booking booking) {
        if (booking.getDriver() == null) return;
        String toEmployee = String.format(
                "Driver %s assigned for your ride on %s. Boarding OTP: %s | Drop OTP: %s. Keep these safe. Ref: %s",
                booking.getDriver().getUser().getFullName(), booking.getScheduledAt(),
                booking.getBoardingOtp(), booking.getDropOtp(), shortId(booking));
        sendSms(booking, booking.getEmployee(), "DRIVER_ASSIGNED", toEmployee);

        String toDriver = String.format("New trip assigned. Pickup: %s at %s. Drop: %s. Ref: %s",
                booking.getPickupAddress(), booking.getScheduledAt(), booking.getDropAddress(), shortId(booking));
        sendSms(booking, booking.getDriver().getUser(), "TRIP_ASSIGNED", toDriver);
    }

    public void notifyDriverEnRoute(Booking booking) {
        if (booking.getDriver() == null) return;
        String msg = String.format("Your driver %s is on the way. Ref: %s",
                booking.getDriver().getUser().getFullName(), shortId(booking));
        sendSms(booking, booking.getEmployee(), "DRIVER_EN_ROUTE", msg);
    }

    public void notifyTripStarted(Booking booking) {
        String msg = String.format("Your trip has started. Have a safe journey! Ref: %s", shortId(booking));
        sendSms(booking, booking.getEmployee(), "TRIP_STARTED", msg);
    }

    public void notifyTripCompleted(Booking booking) {
        String msg = String.format("Your trip is complete. Fare: Rs %.2f. Ref: %s",
                booking.getFinalFare() != null ? booking.getFinalFare() : booking.getEstimatedFare(),
                shortId(booking));
        sendSms(booking, booking.getEmployee(), "TRIP_COMPLETED", msg);
    }

    public void notifyBookingCancelled(Booking booking) {
        sendSms(booking, booking.getEmployee(), "BOOKING_CANCELLED",
                String.format("Your booking has been cancelled. Ref: %s", shortId(booking)));
        if (booking.getDriver() != null) {
            sendSms(booking, booking.getDriver().getUser(), "TRIP_CANCELLED",
                    String.format("Trip cancelled. Ref: %s", shortId(booking)));
        }
    }

    public void sendTempPassword(User user, String tempPassword) {
        String msg = String.format("Welcome to CarBooking! Your login: %s, Password: %s. Please change on first login.",
                user.getEmail(), tempPassword);
        sendSms(null, user, "ACCOUNT_CREATED", msg);
    }

    public void sendSosAlert(Tenant tenant, String message) {
        List<User> fleetManagers = userRepository.findByTenantAndRole(tenant, Role.FLEET_MANAGER);
        for (User fm : fleetManagers) {
            sendSms(null, fm, "SOS_ALERT", message);
        }
    }

    public void sendOtpSms(Booking booking, String otp) {
        String msg = String.format("Your boarding OTP is %s. Use it when the driver arrives. Ref: %s", otp, shortId(booking));
        sendSms(booking, booking.getEmployee(), "BOARDING_OTP", msg);
    }

    public void sendDirectSms(Tenant tenant, User recipient, String eventType, String message) {
        sendSms(null, recipient, eventType, message);
    }

    public void notifyDailyTripScheduled(DailyTripPassenger passenger, DailyTrip trip) {
        String msg = String.format(
                "Your cab is scheduled for %s at %s. Boarding OTP: %s | Drop OTP: %s. Keep safe, share only with driver.",
                trip.getTripDate(), trip.getScheduledPickupTime(),
                passenger.getBoardingOtp(), passenger.getDropOtp());
        sendSms(null, passenger.getEmployee(), "DAILY_TRIP_OTP", msg);
    }

    public void notifyDriverDailyTripAssigned(DailyTrip trip, int passengerCount) {
        if (trip.getDriver() == null) return;
        String msg = String.format(
                "Daily trip assigned: %s on %s at %s. %d passenger(s). Report to fleet manager if issues.",
                trip.getDailySchedule().getName(), trip.getTripDate(),
                trip.getScheduledPickupTime(), passengerCount);
        sendSms(null, trip.getDriver().getUser(), "DAILY_TRIP_ASSIGNED", msg);
    }

    public void notifyUnassignedDailyTrip(DailyTrip trip) {
        List<User> managers = userRepository.findByTenantAndRole(trip.getTenant(), Role.FLEET_MANAGER);
        String msg = String.format(
                "⚠️ No driver assigned for trip '%s' on %s at %s. Please assign immediately.",
                trip.getDailySchedule().getName(), trip.getTripDate(), trip.getScheduledPickupTime());
        for (User manager : managers) {
            sendSms(null, manager, "TRIP_UNASSIGNED_ALERT", msg);
        }
    }

    public void notifyPassengerTripCancelled(DailyTripPassenger passenger, DailyTrip trip) {
        String msg = String.format("Your cab for %s at %s has been cancelled. Contact your admin.",
                trip.getTripDate(), trip.getScheduledPickupTime());
        sendSms(null, passenger.getEmployee(), "DAILY_TRIP_CANCELLED", msg);
    }

    // ── Core send ──────────────────────────────────────────────────────────

    private void sendSms(Booking booking, User recipient, String eventType, String message) {
        if (recipient == null || recipient.getPhone() == null) return;

        NotificationLog record = NotificationLog.builder()
                .tenant(booking != null ? booking.getTenant() : recipient.getTenant())
                .booking(booking)
                .recipient(recipient)
                .channel(NotificationChannel.SMS)
                .eventType(eventType)
                .body(message)
                .status("PENDING")
                .build();

        if (msg91AuthKey == null || msg91AuthKey.isBlank()) {
            record.setStatus("SKIPPED");
            record.setFailureReason("MSG91_AUTH_KEY not configured");
            notificationLogRepository.save(record);
            log.info("SMS [{}] to {}: {}", eventType, recipient.getPhone(), message);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authkey", msg91AuthKey);

            Map<String, Object> body = Map.of(
                    "sender", senderId,
                    "route", "4",
                    "country", "91",
                    "sms", new Object[]{
                            Map.of("message", message, "to", new String[]{recipient.getPhone()})
                    }
            );

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/flow/",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                record.setStatus("SENT");
                record.setSentAt(Instant.now());
            } else {
                record.setStatus("FAILED");
                record.setFailureReason("HTTP " + response.getStatusCode());
            }
        } catch (Exception e) {
            record.setStatus("FAILED");
            record.setFailureReason(e.getMessage());
            log.warn("SMS send failed for event {} to {}: {}", eventType, recipient.getPhone(), e.getMessage());
        }

        notificationLogRepository.save(record);
    }

    private String shortId(Booking booking) {
        return booking.getId().toString().substring(0, 8).toUpperCase();
    }
}
