package com.carbooking.service;

import com.carbooking.common.enums.DriverAvailability;
import com.carbooking.common.enums.DailyTripPassengerStatus;
import com.carbooking.common.enums.DailyTripStatus;
import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.request.dailyschedule.AssignDriverToDailyTripRequest;
import com.carbooking.dto.request.dailyschedule.VerifyDailyTripOtpRequest;
import com.carbooking.dto.response.dailyschedule.DailyTripPassengerResponse;
import com.carbooking.dto.response.dailyschedule.DailyTripResponse;
import com.carbooking.dto.response.dailyschedule.MyTodayTripResponse;
import com.carbooking.entity.*;
import com.carbooking.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DailyTripService {

    private final DailyTripRepository dailyTripRepository;
    private final DailyTripPassengerRepository dailyTripPassengerRepository;
    private final DailySchedulePassengerRepository dailySchedulePassengerRepository;
    private final DailyTripSkipDateRepository dailyTripSkipDateRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final CancellationConfigRepository cancellationConfigRepository;
    private final NotificationService notificationService;

    @Autowired
    public DailyTripService(
            DailyTripRepository dailyTripRepository,
            DailyTripPassengerRepository dailyTripPassengerRepository,
            DailySchedulePassengerRepository dailySchedulePassengerRepository,
            DailyTripSkipDateRepository dailyTripSkipDateRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository,
            CancellationConfigRepository cancellationConfigRepository,
            @Lazy NotificationService notificationService) {
        this.dailyTripRepository = dailyTripRepository;
        this.dailyTripPassengerRepository = dailyTripPassengerRepository;
        this.dailySchedulePassengerRepository = dailySchedulePassengerRepository;
        this.dailyTripSkipDateRepository = dailyTripSkipDateRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.cancellationConfigRepository = cancellationConfigRepository;
        this.notificationService = notificationService;
    }

    public List<DailyTripResponse> listByDate(LocalDate date) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        return dailyTripRepository.findByTenantAndTripDate(tenant, date)
                .stream()
                .map(this::toTripResponse)
                .collect(Collectors.toList());
    }

    public DailyTripResponse get(UUID tripId) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        DailyTrip trip = dailyTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily trip not found"));
        if (!trip.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }
        return toTripResponse(trip);
    }

    @Transactional
    public DailyTripResponse assignDriver(UUID tripId, AssignDriverToDailyTripRequest request) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        DailyTrip trip = dailyTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily trip not found"));
        if (!trip.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        if (!driver.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }
        if (driver.getAvailability() != DriverAvailability.AVAILABLE) {
            throw new BusinessRuleException("Driver is not available");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        if (!vehicle.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new BusinessRuleException("Vehicle is not active");
        }

        trip.setDriver(driver);
        trip.setVehicle(vehicle);
        trip.setStatus(DailyTripStatus.DRIVER_ASSIGNED);

        driver.setAvailability(DriverAvailability.ON_TRIP);
        vehicle.setStatus(VehicleStatus.IN_TRIP);

        driverRepository.save(driver);
        vehicleRepository.save(vehicle);
        trip = dailyTripRepository.save(trip);

        int passengerCount = dailyTripPassengerRepository.findByDailyTripOrderByStopSequenceAsc(trip).size();
        notificationService.notifyDriverDailyTripAssigned(trip, passengerCount);

        return toTripResponse(trip);
    }

    @Transactional
    public void cancelTrip(UUID tripId) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        DailyTrip trip = dailyTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily trip not found"));
        if (!trip.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        // Cancel all SCHEDULED passengers
        List<DailyTripPassenger> passengers = dailyTripPassengerRepository
                .findByDailyTripAndStatusIn(trip, Arrays.asList(DailyTripPassengerStatus.SCHEDULED));
        for (DailyTripPassenger p : passengers) {
            p.setStatus(DailyTripPassengerStatus.CANCELLED);
            p.setCancelledAt(Instant.now());
            dailyTripPassengerRepository.save(p);
            notificationService.notifyPassengerTripCancelled(p, trip);
        }

        trip.setStatus(DailyTripStatus.CANCELLED);

        // Free driver and vehicle if assigned
        if (trip.getDriver() != null) {
            Driver driver = trip.getDriver();
            driver.setAvailability(DriverAvailability.AVAILABLE);
            driverRepository.save(driver);
        }
        if (trip.getVehicle() != null) {
            Vehicle vehicle = trip.getVehicle();
            vehicle.setStatus(VehicleStatus.ACTIVE);
            vehicleRepository.save(vehicle);
        }

        dailyTripRepository.save(trip);
    }

    public DailyTripResponse getMyTodayTrip() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Driver driver = driverRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        LocalDate today = LocalDate.now(ZoneId.of(tenant.getTimezone()));

        DailyTrip trip = dailyTripRepository.findFirstByDriverAndTripDateAndStatusIn(
                driver, today,
                Arrays.asList(DailyTripStatus.DRIVER_ASSIGNED, DailyTripStatus.IN_PROGRESS))
                .orElseThrow(() -> new ResourceNotFoundException("No trip found for today"));

        return toTripResponse(trip);
    }

    @Transactional
    public DailyTripResponse boardPassenger(UUID tripId, UUID passengerId, VerifyDailyTripOtpRequest request) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        DailyTrip trip = dailyTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily trip not found"));
        if (!trip.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        // Verify caller is the assigned driver
        UUID userId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Driver driver = driverRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("You are not the assigned driver for this trip");
        }

        DailyTripPassenger passenger = dailyTripPassengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found"));
        if (!passenger.getDailyTrip().getId().equals(tripId)) {
            throw new UnauthorizedException("Access denied");
        }

        if (passenger.getStatus() != DailyTripPassengerStatus.SCHEDULED) {
            throw new BusinessRuleException("Passenger is not in SCHEDULED status");
        }
        if (!passenger.getBoardingOtp().equals(request.getOtp())) {
            throw new BusinessRuleException("Invalid boarding OTP");
        }

        passenger.setStatus(DailyTripPassengerStatus.BOARDED);
        passenger.setBoardingVerifiedAt(Instant.now());
        dailyTripPassengerRepository.save(passenger);

        if (trip.getStatus() == DailyTripStatus.DRIVER_ASSIGNED) {
            trip.setStatus(DailyTripStatus.IN_PROGRESS);
            trip = dailyTripRepository.save(trip);
        }

        return toTripResponse(trip);
    }

    @Transactional
    public DailyTripResponse dropPassenger(UUID tripId, UUID passengerId, VerifyDailyTripOtpRequest request) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        DailyTrip trip = dailyTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily trip not found"));
        if (!trip.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        UUID userId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Driver driver = driverRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("You are not the assigned driver for this trip");
        }

        DailyTripPassenger passenger = dailyTripPassengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found"));
        if (!passenger.getDailyTrip().getId().equals(tripId)) {
            throw new UnauthorizedException("Access denied");
        }

        if (passenger.getStatus() != DailyTripPassengerStatus.BOARDED) {
            throw new BusinessRuleException("Passenger is not in BOARDED status");
        }
        if (!passenger.getDropOtp().equals(request.getOtp())) {
            throw new BusinessRuleException("Invalid drop OTP");
        }

        passenger.setStatus(DailyTripPassengerStatus.DROPPED);
        passenger.setDropVerifiedAt(Instant.now());
        dailyTripPassengerRepository.save(passenger);

        trip = dailyTripRepository.findById(tripId).orElseThrow();
        checkAndAutoComplete(trip);

        return toTripResponse(dailyTripRepository.findById(tripId).orElseThrow());
    }

    @Transactional
    public DailyTripResponse markNoShow(UUID tripId, UUID passengerId) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        DailyTrip trip = dailyTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily trip not found"));
        if (!trip.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        if (trip.getStatus() != DailyTripStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Trip must be IN_PROGRESS to mark no-show");
        }

        UUID userId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Driver driver = driverRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("You are not the assigned driver for this trip");
        }

        DailyTripPassenger passenger = dailyTripPassengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found"));
        if (!passenger.getDailyTrip().getId().equals(tripId)) {
            throw new UnauthorizedException("Access denied");
        }

        passenger.setStatus(DailyTripPassengerStatus.NO_SHOW);
        dailyTripPassengerRepository.save(passenger);

        checkAndAutoComplete(trip);

        return toTripResponse(dailyTripRepository.findById(tripId).orElseThrow());
    }

    @Transactional
    public DailyTripResponse completeTrip(UUID tripId) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        DailyTrip trip = dailyTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily trip not found"));
        if (!trip.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        trip.setStatus(DailyTripStatus.COMPLETED);
        freeDriverAndVehicle(trip);
        trip = dailyTripRepository.save(trip);

        return toTripResponse(trip);
    }

    public MyTodayTripResponse getMyTodayAsEmployee() {
        UUID userId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        LocalDate today = LocalDate.now(ZoneId.of(tenant.getTimezone()));

        DailyTripPassenger tripPassenger = dailyTripPassengerRepository
                .findByEmployeeAndDailyTrip_TripDate(currentUser, today)
                .orElseThrow(() -> new ResourceNotFoundException("No trip found for today"));

        return toMyTodayTripResponse(tripPassenger);
    }

    public List<MyTodayTripResponse> getMySchedule(LocalDate from, LocalDate to) {
        UUID userId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return dailyTripPassengerRepository
                .findByEmployeeAndDailyTrip_TripDateBetween(currentUser, from, to)
                .stream()
                .map(this::toMyTodayTripResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void skipDate(UUID enrollmentId, LocalDate skipDate) {
        UUID userId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        DailySchedulePassenger enrollment = dailySchedulePassengerRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        if (!enrollment.getEmployee().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }

        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        LocalDate today = LocalDate.now(ZoneId.of(tenant.getTimezone()));
        LocalDate tomorrow = today.plusDays(1);

        if (!skipDate.isAfter(today)) {
            throw new BusinessRuleException("Can only skip future dates");
        }

        // If skipping tomorrow, check cutoff hour
        if (skipDate.equals(tomorrow)) {
            int currentHour = LocalDateTime.now(ZoneId.of(tenant.getTimezone())).getHour();
            CancellationConfig config = cancellationConfigRepository.findByTenant(tenant)
                    .orElse(null);
            int cutoffHour = config != null && config.getSkipCutoffHour() != null ? config.getSkipCutoffHour() : 22;
            if (currentHour >= cutoffHour) {
                throw new BusinessRuleException("Skip cutoff has passed for tomorrow (cutoff: " + cutoffHour + ":00)");
            }
        }

        if (dailyTripSkipDateRepository.existsBySchedulePassengerAndSkipDate(enrollment, skipDate)) {
            throw new BusinessRuleException("Already skipped this date");
        }

        DailyTripSkipDate skipRecord = DailyTripSkipDate.builder()
                .schedulePassenger(enrollment)
                .skipDate(skipDate)
                .skippedBy(currentUser)
                .createdAt(Instant.now())
                .build();
        dailyTripSkipDateRepository.save(skipRecord);

        // If DailyTrip already exists for that date, cancel this passenger's row
        dailyTripPassengerRepository.findByEmployeeAndDailyTrip_TripDate(currentUser, skipDate)
                .ifPresent(tp -> {
                    if (tp.getStatus() == DailyTripPassengerStatus.SCHEDULED) {
                        tp.setStatus(DailyTripPassengerStatus.CANCELLED);
                        tp.setCancelledAt(Instant.now());
                        dailyTripPassengerRepository.save(tp);

                        // Check if all passengers are terminal
                        DailyTrip trip = tp.getDailyTrip();
                        checkAndAutoComplete(trip);
                    }
                });
    }

    @Transactional
    public void undoSkip(UUID enrollmentId, LocalDate skipDate) {
        UUID userId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        DailySchedulePassenger enrollment = dailySchedulePassengerRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        if (!enrollment.getEmployee().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }

        DailyTripSkipDate skipRecord = dailyTripSkipDateRepository
                .findBySchedulePassengerAndSkipDate(enrollment, skipDate)
                .orElseThrow(() -> new ResourceNotFoundException("Skip record not found"));

        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        LocalDate today = LocalDate.now(ZoneId.of(tenant.getTimezone()));
        if (!skipDate.isAfter(today)) {
            throw new BusinessRuleException("Cannot undo skip for past dates");
        }

        // If trip exists and is not in progress/completed/cancelled, recreate passenger row
        enrollment.getDailySchedule(); // ensure loaded
        dailyTripRepository.findByDailyScheduleAndTripDate(enrollment.getDailySchedule(), skipDate)
                .ifPresent(trip -> {
                    if (trip.getStatus() != DailyTripStatus.IN_PROGRESS
                            && trip.getStatus() != DailyTripStatus.COMPLETED
                            && trip.getStatus() != DailyTripStatus.CANCELLED) {
                        // Check if passenger row exists already (and is CANCELLED)
                        dailyTripPassengerRepository.findByDailyTripAndEmployee(trip, currentUser)
                                .ifPresentOrElse(
                                        tp -> {
                                            // Reactivate
                                            String boardingOtp = generateOtp();
                                            String dropOtp = generateOtp();
                                            while (dropOtp.equals(boardingOtp)) dropOtp = generateOtp();
                                            tp.setStatus(DailyTripPassengerStatus.SCHEDULED);
                                            tp.setCancelledAt(null);
                                            tp.setBoardingOtp(boardingOtp);
                                            tp.setDropOtp(dropOtp);
                                            dailyTripPassengerRepository.save(tp);
                                            notificationService.notifyDailyTripScheduled(tp, trip);
                                        },
                                        () -> {
                                            // Create new row
                                            String boardingOtp = generateOtp();
                                            String dropOtp = generateOtp();
                                            while (dropOtp.equals(boardingOtp)) dropOtp = generateOtp();
                                            DailyTripPassenger newPassenger = DailyTripPassenger.builder()
                                                    .tenant(trip.getTenant())
                                                    .dailyTrip(trip)
                                                    .employee(currentUser)
                                                    .pickupAddress(enrollment.getPickupAddress())
                                                    .pickupLat(enrollment.getPickupLat())
                                                    .pickupLng(enrollment.getPickupLng())
                                                    .stopSequence(enrollment.getStopSequence())
                                                    .boardingOtp(boardingOtp)
                                                    .dropOtp(dropOtp)
                                                    .status(DailyTripPassengerStatus.SCHEDULED)
                                                    .build();
                                            dailyTripPassengerRepository.save(newPassenger);
                                            notificationService.notifyDailyTripScheduled(newPassenger, trip);
                                        });
                    }
                });

        dailyTripSkipDateRepository.delete(skipRecord);
    }

    public List<DailyTripSkipDate> listSkipDates(UUID enrollmentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        DailySchedulePassenger enrollment = dailySchedulePassengerRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        if (!enrollment.getEmployee().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }

        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        LocalDate today = LocalDate.now(ZoneId.of(tenant.getTimezone()));
        return dailyTripSkipDateRepository
                .findBySchedulePassengerAndSkipDateGreaterThanEqual(enrollment, today);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private String generateOtp() {
        return String.format("%04d", new java.util.Random().nextInt(10000));
    }

    private DailyTripResponse toTripResponse(DailyTrip trip) {
        List<DailyTripPassengerResponse> passengers = dailyTripPassengerRepository
                .findByDailyTripOrderByStopSequenceAsc(trip)
                .stream()
                .map(this::toPassengerResponse)
                .collect(Collectors.toList());

        return DailyTripResponse.builder()
                .id(trip.getId())
                .dailyScheduleId(trip.getDailySchedule().getId())
                .scheduleName(trip.getDailySchedule().getName())
                .tripDate(trip.getTripDate())
                .scheduledPickupTime(trip.getScheduledPickupTime())
                .dropAddress(trip.getDropAddress())
                .driverId(trip.getDriver() != null ? trip.getDriver().getId() : null)
                .driverName(trip.getDriver() != null ? trip.getDriver().getUser().getFullName() : null)
                .driverPhone(trip.getDriver() != null ? trip.getDriver().getUser().getPhone() : null)
                .vehicleId(trip.getVehicle() != null ? trip.getVehicle().getId() : null)
                .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                .status(trip.getStatus())
                .passengers(passengers)
                .createdAt(trip.getCreatedAt())
                .build();
    }

    private DailyTripPassengerResponse toPassengerResponse(DailyTripPassenger p) {
        return DailyTripPassengerResponse.builder()
                .id(p.getId())
                .employeeUserId(p.getEmployee().getId())
                .employeeName(p.getEmployee().getFullName())
                .employeePhone(p.getEmployee().getPhone())
                .pickupAddress(p.getPickupAddress())
                .stopSequence(p.getStopSequence())
                .boardingOtp(p.getBoardingOtp())
                .dropOtp(p.getDropOtp())
                .status(p.getStatus())
                .boardingVerifiedAt(p.getBoardingVerifiedAt())
                .dropVerifiedAt(p.getDropVerifiedAt())
                .cancelledAt(p.getCancelledAt())
                .build();
    }

    private MyTodayTripResponse toMyTodayTripResponse(DailyTripPassenger p) {
        DailyTrip trip = p.getDailyTrip();
        return MyTodayTripResponse.builder()
                .tripId(trip.getId())
                .passengerId(p.getId())
                .scheduleName(trip.getDailySchedule().getName())
                .tripDate(trip.getTripDate())
                .pickupTime(trip.getScheduledPickupTime())
                .pickupAddress(p.getPickupAddress())
                .dropAddress(trip.getDropAddress())
                .driverName(trip.getDriver() != null ? trip.getDriver().getUser().getFullName() : null)
                .driverPhone(trip.getDriver() != null ? trip.getDriver().getUser().getPhone() : null)
                .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                .boardingOtp(p.getBoardingOtp())
                .dropOtp(p.getDropOtp())
                .passengerStatus(p.getStatus())
                .tripStatus(trip.getStatus())
                .build();
    }

    private void checkAndAutoComplete(DailyTrip trip) {
        List<DailyTripPassenger> passengers = dailyTripPassengerRepository
                .findByDailyTripOrderByStopSequenceAsc(trip);

        boolean allTerminal = passengers.stream().allMatch(p ->
                p.getStatus() == DailyTripPassengerStatus.DROPPED
                        || p.getStatus() == DailyTripPassengerStatus.NO_SHOW
                        || p.getStatus() == DailyTripPassengerStatus.CANCELLED);

        if (allTerminal && !passengers.isEmpty()) {
            trip.setStatus(DailyTripStatus.COMPLETED);
            freeDriverAndVehicle(trip);
            dailyTripRepository.save(trip);
            log.info("Auto-completed trip {} — all passengers in terminal state", trip.getId());
        } else if (passengers.isEmpty()) {
            trip.setStatus(DailyTripStatus.CANCELLED);
            freeDriverAndVehicle(trip);
            dailyTripRepository.save(trip);
            log.info("Auto-cancelled trip {} — no passengers", trip.getId());
        }
    }

    private void freeDriverAndVehicle(DailyTrip trip) {
        if (trip.getDriver() != null) {
            Driver driver = trip.getDriver();
            driver.setAvailability(DriverAvailability.AVAILABLE);
            driverRepository.save(driver);
        }
        if (trip.getVehicle() != null) {
            Vehicle vehicle = trip.getVehicle();
            vehicle.setStatus(VehicleStatus.ACTIVE);
            vehicleRepository.save(vehicle);
        }
    }
}
