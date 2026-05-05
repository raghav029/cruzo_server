package com.carbooking.modules.booking.application;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.enums.DriverAvailability;
import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Booking;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.booking.dto.request.CancelBookingRequest;
import com.carbooking.modules.booking.dto.response.BookingResponse;
import com.carbooking.modules.booking.application.BookingMapper;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.config.domain.port.CancellationConfigPort;
import com.carbooking.modules.fleet.domain.port.DriverPort;
import com.carbooking.modules.fleet.domain.port.VehiclePort;
import com.carbooking.modules.notification.application.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class BookingCancellationService extends TenantSupport {

    private final BookingPort bookingRepository;
    private final CancellationConfigPort cancellationConfigRepository;
    private final DriverPort driverRepository;
    private final VehiclePort vehicleRepository;
    private final BookingHistoryHelper historyHelper;
    private final NotificationService notificationService;
    private final BookingMapper bookingMapper;

    public BookingCancellationService(
            BookingPort bookingRepository,
            CancellationConfigPort cancellationConfigRepository,
            DriverPort driverRepository,
            VehiclePort vehicleRepository,
            BookingHistoryHelper historyHelper,
            NotificationService notificationService,
            BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.cancellationConfigRepository = cancellationConfigRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.historyHelper = historyHelper;
        this.notificationService = notificationService;
        this.bookingMapper = bookingMapper;
    }

    @Transactional
    public BookingResponse cancel(UUID bookingId, CancelBookingRequest request) {
        Booking booking = findAndVerify(bookingId);
        String role = currentRole();

        if (booking.getStatus() == BookingStatus.IN_PROGRESS || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot cancel a booking that is in progress or completed");
        }

        if (!"ROLE_FLEET_MANAGER".equals(role)) {
            Tenant tenant = requireTenant();
            cancellationConfigRepository.findByTenant(tenant).ifPresent(config -> {
                boolean withinWindow = booking.getScheduledAt()
                        .isAfter(Instant.now().plus(config.getCancellationWindowHours().longValue(), ChronoUnit.HOURS));
                if (!withinWindow && !config.isAfterWindowAllowed()) {
                    throw new BusinessRuleException("Cancellation window has passed");
                }
            });
        }

        BookingStatus cancelStatus = resolveCancelStatus(role);
        BookingStatus from = booking.getStatus();

        booking.setStatus(cancelStatus);
        booking.setCancellationReason(request.getReason());
        booking.setCancelledAt(Instant.now());

        releaseDriverAndVehicleIfAssigned(booking, from);
        bookingRepository.save(booking);
        historyHelper.append(booking, from, cancelStatus, currentUser(), request.getReason());
        notificationService.notifyBookingCancelled(booking);

        return bookingMapper.toResponse(booking);
    }

    private void releaseDriverAndVehicleIfAssigned(Booking booking, BookingStatus from) {
        boolean wasAssigned = from == BookingStatus.DRIVER_ASSIGNED
                || from == BookingStatus.DRIVER_EN_ROUTE
                || from == BookingStatus.ARRIVED;
        if (booking.getDriver() != null && wasAssigned) {
            booking.getDriver().setAvailability(DriverAvailability.AVAILABLE);
            driverRepository.save(booking.getDriver());
        }
        if (booking.getVehicle() != null && wasAssigned) {
            booking.getVehicle().setStatus(VehicleStatus.ACTIVE);
            vehicleRepository.save(booking.getVehicle());
        }
    }

    private BookingStatus resolveCancelStatus(String role) {
        return switch (role) {
            case "ROLE_EMPLOYEE"       -> BookingStatus.CANCELLED_BY_EMPLOYEE;
            case "ROLE_CORPORATE_ADMIN"-> BookingStatus.CANCELLED_BY_ADMIN;
            case "ROLE_FLEET_MANAGER"  -> BookingStatus.CANCELLED_BY_FLEET_MANAGER;
            case "ROLE_DRIVER"         -> BookingStatus.CANCELLED_BY_DRIVER;
            default -> throw new UnauthorizedException("Role cannot cancel bookings: " + role);
        };
    }

    private Booking findAndVerify(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        assertSameTenant(booking.getTenant().getId());
        UUID userId = currentUserId();
        String role = currentRole();
        if ("ROLE_EMPLOYEE".equals(role) && !booking.getEmployee().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
        return booking;
    }
}
