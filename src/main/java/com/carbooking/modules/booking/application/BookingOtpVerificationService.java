package com.carbooking.modules.booking.application;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Booking;
import com.carbooking.entity.Driver;
import com.carbooking.modules.booking.dto.request.VerifyOtpRequest;
import com.carbooking.modules.booking.dto.response.BookingResponse;
import com.carbooking.modules.booking.application.BookingMapper;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.fleet.domain.port.DriverPort;
import com.carbooking.modules.notification.application.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class BookingOtpVerificationService extends TenantSupport {

    private final BookingPort bookingRepository;
    private final DriverPort driverRepository;
    private final BookingHistoryHelper historyHelper;
    private final NotificationService notificationService;
    private final BookingMapper bookingMapper;

    public BookingOtpVerificationService(
            BookingPort bookingRepository,
            DriverPort driverRepository,
            BookingHistoryHelper historyHelper,
            NotificationService notificationService,
            BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.driverRepository = driverRepository;
        this.historyHelper = historyHelper;
        this.notificationService = notificationService;
        this.bookingMapper = bookingMapper;
    }

    @Transactional
    public BookingResponse verifyOtp(UUID bookingId, VerifyOtpRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        assertSameTenant(booking.getTenant().getId());

        Driver driver = driverRepository.findByUser(currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if (booking.getDriver() == null || !booking.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("You are not assigned to this booking");
        }

        if (booking.getStatus() != BookingStatus.ARRIVED) {
            throw new BusinessRuleException("Booking is not in ARRIVED status");
        }

        if (booking.getDropOtp() == null || !booking.getDropOtp().equals(request.getOtp())) {
            throw new BusinessRuleException("Invalid or expired OTP");
        }

        if (booking.getOtpGeneratedAt() == null || booking.getOtpGeneratedAt().isBefore(Instant.now().minusSeconds(600))) {
            throw new BusinessRuleException("Invalid or expired OTP");
        }

        BookingStatus from = booking.getStatus();
        booking.setOtpVerifiedAt(Instant.now());
        booking.setDropOtp(null);
        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setTripStartedAt(Instant.now());

        bookingRepository.save(booking);
        historyHelper.append(booking, from, BookingStatus.IN_PROGRESS, currentUser(), "OTP verified");
        notificationService.notifyTripStarted(booking);

        return bookingMapper.toResponse(booking);
    }
}
