package com.carbooking.modules.dailyschedule.application;

import com.carbooking.common.enums.*;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.*;
import com.carbooking.modules.dailyschedule.dto.request.AssignDriverToDailyTripRequest;
import com.carbooking.modules.dailyschedule.dto.request.VerifyDailyTripOtpRequest;
import com.carbooking.modules.dailyschedule.dto.response.DailyTripPassengerResponse;
import com.carbooking.modules.dailyschedule.dto.response.DailyTripResponse;
import com.carbooking.modules.dailyschedule.dto.response.MyTodayTripResponse;
import com.carbooking.modules.notification.application.NotificationService;
import com.carbooking.modules.config.domain.port.CancellationConfigPort;
import com.carbooking.modules.dailyschedule.domain.port.DailySchedulePassengerPort;
import com.carbooking.modules.dailyschedule.domain.port.DailyTripPassengerPort;
import com.carbooking.modules.dailyschedule.domain.port.DailyTripPort;
import com.carbooking.modules.dailyschedule.domain.port.DailyTripSkipDatePort;
import com.carbooking.modules.fleet.domain.port.DriverPort;
import com.carbooking.modules.fleet.domain.port.VehiclePort;
import lombok.extern.slf4j.Slf4j;
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
import com.carbooking.modules.dailyschedule.application.DailyTripMapper;

@Slf4j
@Service
public class DailyTripService extends TenantSupport {

    private final DailyTripMapper tripMapper;
    private final DailyTripPort dailyTripRepository;
    private final DailyTripPassengerPort dailyTripPassengerRepository;
    private final DailySchedulePassengerPort dailySchedulePassengerRepository;
    private final DailyTripSkipDatePort dailyTripSkipDateRepository;
    private final DriverPort driverRepository;
    private final VehiclePort vehicleRepository;
    private final CancellationConfigPort cancellationConfigRepository;
    private final NotificationService notificationService;

    public DailyTripService(DailyTripPort dailyTripRepository,
                             DailyTripPassengerPort dailyTripPassengerRepository,
                             DailySchedulePassengerPort dailySchedulePassengerRepository,
                             DailyTripSkipDatePort dailyTripSkipDateRepository,
                             DriverPort driverRepository,
                             VehiclePort vehicleRepository,
                             CancellationConfigPort cancellationConfigRepository,
                             @Lazy NotificationService notificationService,
                                 DailyTripMapper tripMapper) {
        this.dailyTripRepository = dailyTripRepository;
        this.dailyTripPassengerRepository = dailyTripPassengerRepository;
        this.dailySchedulePassengerRepository = dailySchedulePassengerRepository;
        this.dailyTripSkipDateRepository = dailyTripSkipDateRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.cancellationConfigRepository = cancellationConfigRepository;
        this.notificationService = notificationService;
        this.tripMapper = tripMapper;
    }

    public List<DailyTripResponse> listByDate(LocalDate date) {
        Tenant tenant = requireTenant();
        return dailyTripRepository.findByTenantAndTripDate(tenant, date)
                .stream().map(this::buildTripResponse).collect(Collectors.toList());
    }

    public DailyTripResponse get(UUID tripId) {
        return buildTripResponse(findTrip(tripId));
    }

    @Transactional
    public DailyTripResponse assignDriver(UUID tripId, AssignDriverToDailyTripRequest request) {
        Tenant tenant = requireTenant();
        DailyTrip trip = findTrip(tripId);

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        assertSameTenant(driver.getTenant().getId());
        if (driver.getAvailability() != DriverAvailability.AVAILABLE) {
            throw new BusinessRuleException("Driver is not available");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        assertSameTenant(vehicle.getTenant().getId());
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

        return buildTripResponse(trip);
    }

    @Transactional
    public void cancelTrip(UUID tripId) {
        DailyTrip trip = findTrip(tripId);

        List<DailyTripPassenger> passengers = dailyTripPassengerRepository
                .findByDailyTripAndStatusIn(trip, Arrays.asList(DailyTripPassengerStatus.SCHEDULED));
        for (DailyTripPassenger p : passengers) {
            p.setStatus(DailyTripPassengerStatus.CANCELLED);
            p.setCancelledAt(Instant.now());
            dailyTripPassengerRepository.save(p);
            notificationService.notifyPassengerTripCancelled(p, trip);
        }

        trip.setStatus(DailyTripStatus.CANCELLED);
        freeDriverAndVehicle(trip);
        dailyTripRepository.save(trip);
    }

    public DailyTripResponse getMyTodayTrip() {
        User currentUser = currentUser();
        Tenant tenant = requireTenant();

        Driver driver = driverRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        LocalDate today = LocalDate.now(ZoneId.of(tenant.getTimezone()));

        DailyTrip trip = dailyTripRepository.findFirstByDriverAndTripDateAndStatusIn(
                driver, today,
                Arrays.asList(DailyTripStatus.DRIVER_ASSIGNED, DailyTripStatus.IN_PROGRESS))
                .orElseThrow(() -> new ResourceNotFoundException("No trip found for today"));

        return buildTripResponse(trip);
    }

    @Transactional
    public DailyTripResponse boardPassenger(UUID tripId, UUID passengerId, VerifyDailyTripOtpRequest request) {
        DailyTrip trip = findTrip(tripId);
        User currentUser = currentUser();

        Driver driver = driverRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("You are not the assigned driver for this trip");
        }

        DailyTripPassenger passenger = getPassengerForTrip(passengerId, tripId);

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
            dailyTripRepository.save(trip);
        }

        return buildTripResponse(dailyTripRepository.findById(tripId).orElseThrow());
    }

    @Transactional
    public DailyTripResponse dropPassenger(UUID tripId, UUID passengerId, VerifyDailyTripOtpRequest request) {
        DailyTrip trip = findTrip(tripId);
        User currentUser = currentUser();

        Driver driver = driverRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("You are not the assigned driver for this trip");
        }

        DailyTripPassenger passenger = getPassengerForTrip(passengerId, tripId);

        if (passenger.getStatus() != DailyTripPassengerStatus.BOARDED) {
            throw new BusinessRuleException("Passenger is not in BOARDED status");
        }
        if (!passenger.getDropOtp().equals(request.getOtp())) {
            throw new BusinessRuleException("Invalid drop OTP");
        }

        passenger.setStatus(DailyTripPassengerStatus.DROPPED);
        passenger.setDropVerifiedAt(Instant.now());
        dailyTripPassengerRepository.save(passenger);

        DailyTrip refreshed = dailyTripRepository.findById(tripId).orElseThrow();
        checkAndAutoComplete(refreshed);

        return buildTripResponse(dailyTripRepository.findById(tripId).orElseThrow());
    }

    @Transactional
    public DailyTripResponse markNoShow(UUID tripId, UUID passengerId) {
        DailyTrip trip = findTrip(tripId);

        if (trip.getStatus() != DailyTripStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Trip must be IN_PROGRESS to mark no-show");
        }

        User currentUser = currentUser();
        Driver driver = driverRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        if (trip.getDriver() == null || !trip.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("You are not the assigned driver for this trip");
        }

        DailyTripPassenger passenger = getPassengerForTrip(passengerId, tripId);
        passenger.setStatus(DailyTripPassengerStatus.NO_SHOW);
        dailyTripPassengerRepository.save(passenger);

        checkAndAutoComplete(trip);

        return buildTripResponse(dailyTripRepository.findById(tripId).orElseThrow());
    }

    @Transactional
    public DailyTripResponse completeTrip(UUID tripId) {
        DailyTrip trip = findTrip(tripId);
        trip.setStatus(DailyTripStatus.COMPLETED);
        freeDriverAndVehicle(trip);
        return buildTripResponse(dailyTripRepository.save(trip));
    }

    public MyTodayTripResponse getMyTodayAsEmployee() {
        User currentUser = currentUser();
        Tenant tenant = requireTenant();
        LocalDate today = LocalDate.now(ZoneId.of(tenant.getTimezone()));

        DailyTripPassenger tripPassenger = dailyTripPassengerRepository
                .findByEmployeeAndDailyTrip_TripDate(currentUser, today)
                .orElseThrow(() -> new ResourceNotFoundException("No trip found for today"));

        return tripMapper.toMyTodayTripResponse(tripPassenger);
    }

    public List<MyTodayTripResponse> getMySchedule(LocalDate from, LocalDate to) {
        User currentUser = currentUser();
        return dailyTripPassengerRepository
                .findByEmployeeAndDailyTrip_TripDateBetween(currentUser, from, to)
                .stream().map(tripMapper::toMyTodayTripResponse).collect(Collectors.toList());
    }

    @Transactional
    public void skipDate(UUID enrollmentId, LocalDate skipDate) {
        User currentUser = currentUser();

        DailySchedulePassenger enrollment = dailySchedulePassengerRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        if (!enrollment.getEmployee().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Access denied");
        }

        Tenant tenant = requireTenant();
        LocalDate today = LocalDate.now(ZoneId.of(tenant.getTimezone()));
        LocalDate tomorrow = today.plusDays(1);

        if (!skipDate.isAfter(today)) {
            throw new BusinessRuleException("Can only skip future dates");
        }

        if (skipDate.equals(tomorrow)) {
            int currentHour = LocalDateTime.now(ZoneId.of(tenant.getTimezone())).getHour();
            CancellationConfig config = cancellationConfigRepository.findByTenant(tenant).orElse(null);
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

        dailyTripPassengerRepository.findByEmployeeAndDailyTrip_TripDate(currentUser, skipDate)
                .ifPresent(tp -> {
                    if (tp.getStatus() == DailyTripPassengerStatus.SCHEDULED) {
                        tp.setStatus(DailyTripPassengerStatus.CANCELLED);
                        tp.setCancelledAt(Instant.now());
                        dailyTripPassengerRepository.save(tp);
                        checkAndAutoComplete(tp.getDailyTrip());
                    }
                });
    }

    @Transactional
    public void undoSkip(UUID enrollmentId, LocalDate skipDate) {
        User currentUser = currentUser();

        DailySchedulePassenger enrollment = dailySchedulePassengerRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        if (!enrollment.getEmployee().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Access denied");
        }

        DailyTripSkipDate skipRecord = dailyTripSkipDateRepository
                .findBySchedulePassengerAndSkipDate(enrollment, skipDate)
                .orElseThrow(() -> new ResourceNotFoundException("Skip record not found"));

        Tenant tenant = requireTenant();
        LocalDate today = LocalDate.now(ZoneId.of(tenant.getTimezone()));
        if (!skipDate.isAfter(today)) {
            throw new BusinessRuleException("Cannot undo skip for past dates");
        }

        enrollment.getDailySchedule();
        dailyTripRepository.findByDailyScheduleAndTripDate(enrollment.getDailySchedule(), skipDate)
                .ifPresent(trip -> {
                    if (trip.getStatus() != DailyTripStatus.IN_PROGRESS
                            && trip.getStatus() != DailyTripStatus.COMPLETED
                            && trip.getStatus() != DailyTripStatus.CANCELLED) {
                        dailyTripPassengerRepository.findByDailyTripAndEmployee(trip, currentUser)
                                .ifPresentOrElse(
                                        tp -> {
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
        User currentUser = currentUser();
        DailySchedulePassenger enrollment = dailySchedulePassengerRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        if (!enrollment.getEmployee().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Access denied");
        }

        Tenant tenant = requireTenant();
        LocalDate today = LocalDate.now(ZoneId.of(tenant.getTimezone()));
        return dailyTripSkipDateRepository.findBySchedulePassengerAndSkipDateGreaterThanEqual(enrollment, today);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private DailyTrip findTrip(UUID tripId) {
        DailyTrip trip = dailyTripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily trip not found"));
        assertSameTenant(trip.getTenant().getId());
        return trip;
    }

    private DailyTripPassenger getPassengerForTrip(UUID passengerId, UUID tripId) {
        DailyTripPassenger passenger = dailyTripPassengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found"));
        if (!passenger.getDailyTrip().getId().equals(tripId)) {
            throw new UnauthorizedException("Access denied");
        }
        return passenger;
    }

    private String generateOtp() {
        return String.format("%04d", new java.util.Random().nextInt(10000));
    }

    private void checkAndAutoComplete(DailyTrip trip) {
        List<DailyTripPassenger> passengers = dailyTripPassengerRepository.findByDailyTripOrderByStopSequenceAsc(trip);

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

    private DailyTripResponse buildTripResponse(DailyTrip trip) {
        List<DailyTripPassengerResponse> passengers = dailyTripPassengerRepository
                .findByDailyTripOrderByStopSequenceAsc(trip)
                .stream().map(tripMapper::toPassengerResponse).collect(Collectors.toList());
        return tripMapper.toTripResponse(trip, passengers);
    }
}
