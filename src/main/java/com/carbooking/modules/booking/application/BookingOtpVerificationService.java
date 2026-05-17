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
import com.carbooking.modules.booking.dto.request.VerifyOtpRequest;
import com.carbooking.modules.booking.dto.response.BookingResponse;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.config.domain.port.PricingConfigPort;
import com.carbooking.modules.fleet.domain.port.DriverPort;
import com.carbooking.modules.fleet.domain.port.VehiclePort;
import com.carbooking.modules.notification.application.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Service
public class BookingOtpVerificationService extends TenantSupport {

    private final BookingPort bookingRepository;
    private final DriverPort driverRepository;
    private final VehiclePort vehicleRepository;
    private final PricingConfigPort pricingConfigRepository;
    private final BookingHistoryHelper historyHelper;
    private final NotificationService notificationService;
    private final BookingMapper bookingMapper;

    public BookingOtpVerificationService(
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
    public BookingResponse verifyBoardingOtp(UUID bookingId, VerifyOtpRequest request) {
        Booking booking = findAndVerifyDriver(bookingId);

        if (booking.getStatus() != BookingStatus.ARRIVED) {
            throw new BusinessRuleException("Booking must be in ARRIVED status to verify boarding OTP");
        }

        if (booking.getBoardingOtp() == null || !booking.getBoardingOtp().equals(request.getOtp())) {
            throw new BusinessRuleException("Invalid boarding OTP");
        }

        checkOtpExpiry(booking);

        BookingStatus from = booking.getStatus();
        booking.setBoardingOtp(null);
        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setTripStartedAt(Instant.now());

        bookingRepository.save(booking);
        historyHelper.append(booking, from, BookingStatus.IN_PROGRESS, currentUser(), "Boarding OTP verified");
        notificationService.notifyTripStarted(booking);

        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public BookingResponse verifyDropOtp(UUID bookingId, VerifyOtpRequest request) {
        Booking booking = findAndVerifyDriver(bookingId);

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Booking must be IN_PROGRESS to verify drop OTP");
        }

        if (booking.getDropOtp() == null || !booking.getDropOtp().equals(request.getOtp())) {
            throw new BusinessRuleException("Invalid drop OTP");
        }

        Driver driver = driverRepository.findByUser(currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        BookingStatus from = booking.getStatus();
        booking.setDropOtp(null);
        booking.setOtpVerifiedAt(Instant.now());
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setTripCompletedAt(Instant.now());
        booking.setFinalFare(calculateFinalFare(booking));

        driver.setAvailability(DriverAvailability.AVAILABLE);
        driverRepository.save(driver);
        booking.getVehicle().setStatus(VehicleStatus.ACTIVE);
        vehicleRepository.save(booking.getVehicle());

        bookingRepository.save(booking);
        historyHelper.append(booking, from, BookingStatus.COMPLETED, currentUser(), "Drop OTP verified");
        notificationService.notifyTripCompleted(booking);

        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public BookingResponse refreshBoardingOtp(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertSameTenant(booking.getTenant().getId());

        if (!booking.getEmployee().getId().equals(currentUserId()))
            throw new UnauthorizedException("Access denied");

        if (booking.getStatus() != BookingStatus.ARRIVED)
            throw new BusinessRuleException("OTP can only be refreshed when driver has arrived");

        Random rng = new Random();
        String newOtp;
        do {
            newOtp = String.format("%04d", rng.nextInt(10000));
        } while (newOtp.equals(booking.getDropOtp()));

        booking.setBoardingOtp(newOtp);
        booking.setOtpGeneratedAt(Instant.now());
        bookingRepository.save(booking);
        return bookingMapper.toResponse(booking);
    }

    private Booking findAndVerifyDriver(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        assertSameTenant(booking.getTenant().getId());

        Driver driver = driverRepository.findByUser(currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if (booking.getDriver() == null || !booking.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("You are not assigned to this booking");
        }
        return booking;
    }

    private void checkOtpExpiry(Booking booking) {
        if (booking.getOtpGeneratedAt() == null ||
                booking.getOtpGeneratedAt().isBefore(Instant.now().minusSeconds(600))) {
            throw new BusinessRuleException("OTP has expired");
        }
    }

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
                        case SEDAN  -> config.getSedanMultiplier();
                        case SUV    -> config.getSuvMultiplier();
                        case LUXURY -> config.getLuxuryMultiplier();
                    };
                    return config.getBaseFare()
                            .add(config.getPerHourRate().multiply(hours))
                            .multiply(multiplier)
                            .max(config.getMinimumFare())
                            .setScale(2, RoundingMode.HALF_UP);
                })
                .orElse(booking.getEstimatedFare());
    }
}
