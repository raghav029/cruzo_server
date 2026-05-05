package com.carbooking.modules.booking.application;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.enums.DriverAvailability;
import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Booking;
import com.carbooking.entity.Driver;
import com.carbooking.modules.booking.dto.response.BookingResponse;
import com.carbooking.modules.booking.application.BookingMapper;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.fleet.domain.port.DriverPort;
import com.carbooking.modules.config.domain.port.PricingConfigPort;
import com.carbooking.modules.fleet.domain.port.VehiclePort;
import com.carbooking.modules.notification.application.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class BookingDriverStatusService extends TenantSupport {

    private final BookingPort bookingRepository;
    private final DriverPort driverRepository;
    private final VehiclePort vehicleRepository;
    private final PricingConfigPort pricingConfigRepository;
    private final BookingHistoryHelper historyHelper;
    private final NotificationService notificationService;
    private final BookingMapper bookingMapper;

    public BookingDriverStatusService(
            BookingPort bookingRepository,
            DriverPort driverRepository,
            VehiclePort vehicleRepository,
            PricingConfigPort pricingConfigRepository,
            BookingHistoryHelper historyHelper,
            NotificationService notificationService,
            BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.pricingConfigRepository = pricingConfigRepository;
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
            String otp = String.format("%04d", new java.util.Random().nextInt(10000));
            booking.setDropOtp(otp);
            booking.setOtpGeneratedAt(Instant.now());
            bookingRepository.save(booking);
            historyHelper.append(booking, from, to, currentUser(), null);
            notificationService.sendOtpSms(booking, otp);
            notificationService.notifyDriverEnRoute(booking);
            return bookingMapper.toResponse(booking);
        }

        if (to == BookingStatus.IN_PROGRESS) booking.setTripStartedAt(Instant.now());
        if (to == BookingStatus.COMPLETED) {
            booking.setTripCompletedAt(Instant.now());
            booking.setFinalFare(calculateFinalFare(booking));
            driver.setAvailability(DriverAvailability.AVAILABLE);
            driverRepository.save(driver);
            booking.getVehicle().setStatus(VehicleStatus.ACTIVE);
            vehicleRepository.save(booking.getVehicle());
        }

        bookingRepository.save(booking);
        historyHelper.append(booking, from, to, currentUser(), null);

        if (to == BookingStatus.DRIVER_EN_ROUTE) notificationService.notifyDriverEnRoute(booking);
        else if (to == BookingStatus.IN_PROGRESS)  notificationService.notifyTripStarted(booking);
        else if (to == BookingStatus.COMPLETED)    notificationService.notifyTripCompleted(booking);

        return bookingMapper.toResponse(booking);
    }

    private Driver resolveCallerDriver() {
        return driverRepository.findByUser(currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
    }

    private BookingStatus parseDriverTransition(BookingStatus current, String target) {
        return switch (current) {
            case DRIVER_ASSIGNED -> "EN_ROUTE".equals(target)  ? BookingStatus.DRIVER_EN_ROUTE
                    : fail("Invalid transition from DRIVER_ASSIGNED: " + target);
            case DRIVER_EN_ROUTE -> "ARRIVED".equals(target)   ? BookingStatus.ARRIVED
                    : fail("Invalid transition from DRIVER_EN_ROUTE: " + target);
            case ARRIVED         -> "IN_PROGRESS".equals(target) ? BookingStatus.IN_PROGRESS
                    : fail("Invalid transition from ARRIVED: " + target);
            case IN_PROGRESS     -> "COMPLETED".equals(target) ? BookingStatus.COMPLETED
                    : fail("Invalid transition from IN_PROGRESS: " + target);
            default -> throw new BusinessRuleException("Driver cannot transition from status: " + current);
        };
    }

    private BookingStatus fail(String msg) { throw new BusinessRuleException(msg); }

    private BigDecimal calculateFinalFare(Booking booking) {
        if (booking.getTripStartedAt() == null || booking.getTripCompletedAt() == null) {
            return booking.getEstimatedFare();
        }
        return pricingConfigRepository.findByTenant(booking.getTenant())
                .map(config -> {
                    long minutes = java.time.Duration.between(
                            booking.getTripStartedAt(), booking.getTripCompletedAt()).toMinutes();
                    BigDecimal hours = new BigDecimal(minutes).divide(new BigDecimal("60"), 4, RoundingMode.HALF_UP);
                    BigDecimal multiplier = switch (booking.getVehicleTypeRequested()) {
                        case SEDAN   -> config.getSedanMultiplier();
                        case SUV     -> config.getSuvMultiplier();
                        case LUXURY  -> config.getLuxuryMultiplier();
                    };
                    return config.getBaseFare()
                            .add(config.getPerHourRate().multiply(hours))
                            .multiply(multiplier)
                            .max(config.getMinimumFare())
                            .setScale(2, RoundingMode.HALF_UP);
                })
                .orElse(booking.getEstimatedFare());
    }

    private Booking findAndVerify(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        assertSameTenant(booking.getTenant().getId());
        return booking;
    }
}
