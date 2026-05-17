package com.carbooking.modules.booking.application;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Booking;
import com.carbooking.entity.Driver;
import com.carbooking.modules.booking.dto.response.BookingResponse;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.fleet.domain.port.DriverPort;
import com.carbooking.modules.notification.application.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class BookingDriverStatusService extends TenantSupport {

    private final BookingPort bookingRepository;
    private final DriverPort driverRepository;
    private final BookingHistoryHelper historyHelper;
    private final NotificationService notificationService;
    private final BookingMapper bookingMapper;

    public BookingDriverStatusService(
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
    public BookingResponse updateDriverStatus(UUID bookingId, String targetStatus) {
        Booking booking = findAndVerify(bookingId);
        Driver driver = resolveCallerDriver();

        if (booking.getDriver() == null || !booking.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("You are not assigned to this booking");
        }

        BookingStatus from = booking.getStatus();
        BookingStatus to = parseDriverTransition(from, targetStatus);
        booking.setStatus(to);

        if (to == BookingStatus.ARRIVED) {
            booking.setOtpGeneratedAt(Instant.now());
        }

        bookingRepository.save(booking);
        historyHelper.append(booking, from, to, currentUser(), null);

        if (to == BookingStatus.DRIVER_EN_ROUTE) notificationService.notifyDriverEnRoute(booking);
        else if (to == BookingStatus.ARRIVED)    notificationService.notifyDriverEnRoute(booking);

        return bookingMapper.toResponse(booking);
    }

    private Driver resolveCallerDriver() {
        return driverRepository.findByUser(currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
    }

    private BookingStatus parseDriverTransition(BookingStatus current, String target) {
        return switch (current) {
            case DRIVER_ASSIGNED -> "EN_ROUTE".equals(target) ? BookingStatus.DRIVER_EN_ROUTE
                    : fail("Invalid transition from DRIVER_ASSIGNED: " + target);
            case DRIVER_EN_ROUTE -> "ARRIVED".equals(target)  ? BookingStatus.ARRIVED
                    : fail("Invalid transition from DRIVER_EN_ROUTE: " + target);
            case ARRIVED     -> throw new BusinessRuleException("Use POST /verify-otp with boarding OTP to start the trip");
            case IN_PROGRESS -> throw new BusinessRuleException("Use POST /verify-drop-otp with drop OTP to complete the trip");
            default -> throw new BusinessRuleException("Driver cannot transition from status: " + current);
        };
    }

    private BookingStatus fail(String msg) { throw new BusinessRuleException(msg); }

    private Booking findAndVerify(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        assertSameTenant(booking.getTenant().getId());
        return booking;
    }
}
