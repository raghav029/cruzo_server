package com.carbooking.service;

import com.carbooking.common.enums.*;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.request.booking.*;
import com.carbooking.dto.response.booking.BookingResponse;
import com.carbooking.dto.response.booking.BookingStatusHistoryResponse;
import com.carbooking.entity.*;
import com.carbooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final TenantRepository tenantRepository;
    private final CorporateClientRepository corporateClientRepository;
    private final CorporateEmployeeRepository employeeRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final CancellationConfigRepository cancellationConfigRepository;
    private final PricingConfigRepository pricingConfigRepository;
    private final NotificationService notificationService;

    public BookingService(BookingRepository bookingRepository,
                          BookingStatusHistoryRepository historyRepository,
                          TenantRepository tenantRepository,
                          CorporateClientRepository corporateClientRepository,
                          CorporateEmployeeRepository employeeRepository,
                          DriverRepository driverRepository,
                          VehicleRepository vehicleRepository,
                          UserRepository userRepository,
                          CancellationConfigRepository cancellationConfigRepository,
                          PricingConfigRepository pricingConfigRepository,
                          @Lazy NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.historyRepository = historyRepository;
        this.tenantRepository = tenantRepository;
        this.corporateClientRepository = corporateClientRepository;
        this.employeeRepository = employeeRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.cancellationConfigRepository = cancellationConfigRepository;
        this.pricingConfigRepository = pricingConfigRepository;
        this.notificationService = notificationService;
    }

    // ── Create (EMPLOYEE) ──────────────────────────────────────────────────

    @Transactional
    public BookingResponse create(CreateBookingRequest request) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        User employee = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CorporateClient client = corporateClientRepository.findById(request.getCorporateClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found"));
        if (!client.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        if (request.getScheduledAt().isBefore(Instant.now().plus(2, ChronoUnit.HOURS))) {
            throw new BusinessRuleException("Booking must be scheduled at least 2 hours in advance");
        }

        BigDecimalHolder estimated = estimateFare(tenant, request.getVehicleTypeRequested());

        Booking booking = Booking.builder()
                .tenant(tenant)
                .corporateClient(client)
                .employee(employee)
                .pickupAddress(request.getPickupAddress())
                .dropAddress(request.getDropAddress())
                .pickupLat(request.getPickupLat())
                .pickupLng(request.getPickupLng())
                .dropLat(request.getDropLat())
                .dropLng(request.getDropLng())
                .vehicleTypeRequested(request.getVehicleTypeRequested())
                .scheduledAt(request.getScheduledAt())
                .notes(request.getNotes())
                .status(BookingStatus.PENDING_APPROVAL)
                .estimatedFare(estimated.value)
                .build();

        booking = bookingRepository.save(booking);
        appendHistory(booking, null, BookingStatus.PENDING_APPROVAL, employee, null);
        notificationService.notifyBookingCreated(booking);

        return toResponse(booking);
    }

    // ── List / Get ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BookingResponse> list(BookingStatus status, Pageable pageable) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        String role = SecurityUtils.getCurrentRole();
        UUID userId = SecurityUtils.getCurrentUserId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        Page<Booking> page;
        if ("ROLE_EMPLOYEE".equals(role)) {
            User employee = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            page = bookingRepository.findByEmployee(employee, pageable);
        } else if (status != null) {
            page = bookingRepository.findByTenantAndStatus(tenant, status, pageable);
        } else {
            page = bookingRepository.findByTenant(tenant, pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse get(UUID bookingId) {
        return toResponse(findAndVerify(bookingId));
    }

    @Transactional(readOnly = true)
    public List<BookingStatusHistoryResponse> getHistory(UUID bookingId) {
        Booking booking = findAndVerify(bookingId);
        return historyRepository.findByBookingOrderByTransitionedAtAsc(booking)
                .stream().map(this::toHistoryResponse).collect(Collectors.toList());
    }

    // ── DRIVER: active trip ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BookingResponse getMyActiveTrip() {
        UUID userId = SecurityUtils.getCurrentUserId();
        Driver driver = driverRepository.findByUser(
                userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")))
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        Booking booking = bookingRepository.findFirstByDriverAndStatusIn(driver, List.of(
                BookingStatus.DRIVER_ASSIGNED,
                BookingStatus.DRIVER_EN_ROUTE,
                BookingStatus.ARRIVED,
                BookingStatus.IN_PROGRESS
        )).orElseThrow(() -> new ResourceNotFoundException("No active trip found"));

        return toResponse(booking);
    }

    @Transactional
    public BookingResponse updateDriverLocation(UUID bookingId, UpdateDriverLocationRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Driver driver = driverRepository.findByUser(
                userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")))
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getDriver() == null || !booking.getDriver().getId().equals(driver.getId()))
            throw new UnauthorizedException("You are not assigned to this booking");

        if (booking.getStatus() != BookingStatus.DRIVER_EN_ROUTE &&
            booking.getStatus() != BookingStatus.ARRIVED &&
            booking.getStatus() != BookingStatus.IN_PROGRESS)
            throw new BusinessRuleException("Location updates only allowed during active trip");

        booking.setDriverCurrentLat(request.getLat());
        booking.setDriverCurrentLng(request.getLng());
        booking.setLocationUpdatedAt(Instant.now());
        bookingRepository.save(booking);

        return toResponse(booking);
    }

    // ── CORPORATE_ADMIN: approve / reject ──────────────────────────────────

    @Transactional
    public BookingResponse approve(UUID bookingId) {
        Booking booking = findAndVerify(bookingId);
        requireStatus(booking, BookingStatus.PENDING_APPROVAL);

        User actor = currentUser();
        booking.setStatus(BookingStatus.APPROVED);
        booking.setApprovedAt(Instant.now());
        bookingRepository.save(booking);
        appendHistory(booking, BookingStatus.PENDING_APPROVAL, BookingStatus.APPROVED, actor, null);
        notificationService.notifyBookingApproved(booking);

        return toResponse(booking);
    }

    @Transactional
    public BookingResponse reject(UUID bookingId, RejectBookingRequest request) {
        Booking booking = findAndVerify(bookingId);
        requireStatus(booking, BookingStatus.PENDING_APPROVAL);

        User actor = currentUser();
        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectionReason(request.getReason());
        bookingRepository.save(booking);
        appendHistory(booking, BookingStatus.PENDING_APPROVAL, BookingStatus.REJECTED, actor, request.getReason());
        notificationService.notifyBookingRejected(booking);

        return toResponse(booking);
    }

    // ── FLEET_MANAGER: assign driver (manual) ─────────────────────────────

    @Transactional
    public BookingResponse assignDriver(UUID bookingId, AssignDriverRequest request) {
        Booking booking = findAndVerify(bookingId);
        requireStatus(booking, BookingStatus.APPROVED);

        UUID tenantId = SecurityUtils.getCurrentTenantId();

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        if (!driver.getTenant().getId().equals(tenantId)) throw new UnauthorizedException("Access denied");
        if (driver.getAvailability() != DriverAvailability.AVAILABLE) {
            throw new BusinessRuleException("Driver is not available");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        if (!vehicle.getTenant().getId().equals(tenantId)) throw new UnauthorizedException("Access denied");
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new BusinessRuleException("Vehicle is not active");
        }

        User actor = currentUser();
        doAssign(booking, driver, vehicle, actor, AssignmentMode.MANUAL);

        return toResponse(booking);
    }

    // ── FLEET_MANAGER: auto-assign ─────────────────────────────────────────

    @Transactional
    public BookingResponse autoAssign(UUID bookingId) {
        Booking booking = findAndVerify(bookingId);
        requireStatus(booking, BookingStatus.APPROVED);

        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        List<Driver> availableDrivers = driverRepository.findAvailableDriversByTenantOrderByLastUpdated(tenant);
        if (availableDrivers.isEmpty()) {
            throw new BusinessRuleException("No available drivers");
        }

        List<Vehicle> vehicles = vehicleRepository.findByTenantAndStatusAndVehicleType(
                tenant, VehicleStatus.ACTIVE, booking.getVehicleTypeRequested());
        if (vehicles.isEmpty()) {
            throw new BusinessRuleException("No active vehicles of type " + booking.getVehicleTypeRequested());
        }

        User actor = currentUser();
        doAssign(booking, availableDrivers.get(0), vehicles.get(0), actor, AssignmentMode.AUTO);

        return toResponse(booking);
    }

    // ── DRIVER: status transitions ─────────────────────────────────────────

    @Transactional
    public BookingResponse updateDriverStatus(UUID bookingId, String targetStatus) {
        Booking booking = findAndVerify(bookingId);
        UUID userId = SecurityUtils.getCurrentUserId();

        Driver driver = driverRepository.findByUser(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")))
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if (booking.getDriver() == null || !booking.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("You are not assigned to this booking");
        }

        BookingStatus from = booking.getStatus();
        BookingStatus to = parseDriverTransition(from, targetStatus);

        booking.setStatus(to);
        if (to == BookingStatus.ARRIVED) {
            // Generate OTP for boarding confirmation
            String otp = String.format("%04d", new java.util.Random().nextInt(10000));
            booking.setDropOtp(otp);
            booking.setOtpGeneratedAt(Instant.now());
            bookingRepository.save(booking);
            notificationService.sendOtpSms(booking, otp);
            appendHistory(booking, from, to, userRepository.findById(userId).orElse(null), null);
            notificationService.notifyDriverEnRoute(booking);
            return toResponse(booking);
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
        appendHistory(booking, from, to, userRepository.findById(userId).orElse(null), null);

        if (to == BookingStatus.DRIVER_EN_ROUTE) notificationService.notifyDriverEnRoute(booking);
        else if (to == BookingStatus.IN_PROGRESS) notificationService.notifyTripStarted(booking);
        else if (to == BookingStatus.COMPLETED) notificationService.notifyTripCompleted(booking);

        return toResponse(booking);
    }

    // ── OTP verification ───────────────────────────────────────────────────

    @Transactional
    public BookingResponse verifyOtp(UUID bookingId, com.carbooking.dto.request.booking.VerifyOtpRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        if (!booking.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }

        UUID userId = SecurityUtils.getCurrentUserId();
        Driver driver = driverRepository.findByUser(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")))
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if (booking.getDriver() == null || !booking.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("You are not assigned to this booking");
        }

        if (booking.getStatus() != BookingStatus.ARRIVED) {
            throw new com.carbooking.common.exception.BusinessRuleException("Booking is not in ARRIVED status");
        }

        if (booking.getDropOtp() == null || !booking.getDropOtp().equals(request.getOtp())) {
            throw new com.carbooking.common.exception.BusinessRuleException("Invalid or expired OTP");
        }

        if (booking.getOtpGeneratedAt() == null ||
                booking.getOtpGeneratedAt().isBefore(Instant.now().minusSeconds(600))) {
            throw new com.carbooking.common.exception.BusinessRuleException("Invalid or expired OTP");
        }

        BookingStatus from = booking.getStatus();
        booking.setOtpVerifiedAt(Instant.now());
        booking.setDropOtp(null);
        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setTripStartedAt(Instant.now());

        bookingRepository.save(booking);
        appendHistory(booking, from, BookingStatus.IN_PROGRESS, userRepository.findById(userId).orElse(null), "OTP verified");
        notificationService.notifyTripStarted(booking);

        return toResponse(booking);
    }

    // ── Cancel ─────────────────────────────────────────────────────────────

    @Transactional
    public BookingResponse cancel(UUID bookingId, CancelBookingRequest request) {
        Booking booking = findAndVerify(bookingId);
        String role = SecurityUtils.getCurrentRole();
        UUID userId = SecurityUtils.getCurrentUserId();

        if (booking.getStatus() == BookingStatus.IN_PROGRESS ||
                booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessRuleException("Cannot cancel a booking that is in progress or completed");
        }

        BookingStatus cancelStatus = resolveCancelStatus(role, booking);

        // Cancellation window check (non-fleet-managers cannot cancel outside the window)
        if (!"ROLE_FLEET_MANAGER".equals(role)) {
            UUID tenantId = SecurityUtils.getCurrentTenantId();
            Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
            cancellationConfigRepository.findByTenant(tenant).ifPresent(config -> {
                boolean withinWindow = booking.getScheduledAt()
                        .isAfter(Instant.now().plus(config.getCancellationWindowHours().longValue(), ChronoUnit.HOURS));
                if (!withinWindow && !config.isAfterWindowAllowed()) {
                    throw new BusinessRuleException("Cancellation window has passed");
                }
            });
        }

        BookingStatus from = booking.getStatus();
        booking.setStatus(cancelStatus);
        booking.setCancellationReason(request.getReason());
        booking.setCancelledAt(Instant.now());

        // Free driver and vehicle if assigned
        if (booking.getDriver() != null &&
                (from == BookingStatus.DRIVER_ASSIGNED || from == BookingStatus.DRIVER_EN_ROUTE || from == BookingStatus.ARRIVED)) {
            Driver driver = booking.getDriver();
            driver.setAvailability(DriverAvailability.AVAILABLE);
            driverRepository.save(driver);
            if (booking.getVehicle() != null) {
                booking.getVehicle().setStatus(VehicleStatus.ACTIVE);
                vehicleRepository.save(booking.getVehicle());
            }
        }

        bookingRepository.save(booking);
        appendHistory(booking, from, cancelStatus, userRepository.findById(userId).orElse(null), request.getReason());
        notificationService.notifyBookingCancelled(booking);

        return toResponse(booking);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private void doAssign(Booking booking, Driver driver, Vehicle vehicle, User actor, AssignmentMode mode) {
        BookingStatus from = booking.getStatus();
        booking.setDriver(driver);
        booking.setVehicle(vehicle);
        booking.setAssignedBy(actor);
        booking.setAssignmentMode(mode);
        booking.setStatus(BookingStatus.DRIVER_ASSIGNED);
        booking.setDriverAssignedAt(Instant.now());

        driver.setAvailability(DriverAvailability.ON_TRIP);
        vehicle.setStatus(VehicleStatus.IN_TRIP);
        driverRepository.save(driver);
        vehicleRepository.save(vehicle);
        bookingRepository.save(booking);
        appendHistory(booking, from, BookingStatus.DRIVER_ASSIGNED, actor, null);
        notificationService.notifyDriverAssigned(booking);
    }

    private BookingStatus parseDriverTransition(BookingStatus current, String target) {
        return switch (current) {
            case DRIVER_ASSIGNED -> {
                if ("EN_ROUTE".equals(target)) yield BookingStatus.DRIVER_EN_ROUTE;
                throw new BusinessRuleException("Invalid transition from DRIVER_ASSIGNED: " + target);
            }
            case DRIVER_EN_ROUTE -> {
                if ("ARRIVED".equals(target)) yield BookingStatus.ARRIVED;
                throw new BusinessRuleException("Invalid transition from DRIVER_EN_ROUTE: " + target);
            }
            case ARRIVED -> {
                if ("IN_PROGRESS".equals(target)) yield BookingStatus.IN_PROGRESS;
                throw new BusinessRuleException("Invalid transition from ARRIVED: " + target);
            }
            case IN_PROGRESS -> {
                if ("COMPLETED".equals(target)) yield BookingStatus.COMPLETED;
                throw new BusinessRuleException("Invalid transition from IN_PROGRESS: " + target);
            }
            default -> throw new BusinessRuleException("Driver cannot transition from status: " + current);
        };
    }

    private BookingStatus resolveCancelStatus(String role, Booking booking) {
        return switch (role) {
            case "ROLE_EMPLOYEE" -> BookingStatus.CANCELLED_BY_EMPLOYEE;
            case "ROLE_CORPORATE_ADMIN" -> BookingStatus.CANCELLED_BY_ADMIN;
            case "ROLE_FLEET_MANAGER" -> BookingStatus.CANCELLED_BY_FLEET_MANAGER;
            case "ROLE_DRIVER" -> BookingStatus.CANCELLED_BY_DRIVER;
            default -> throw new UnauthorizedException("Role cannot cancel bookings: " + role);
        };
    }

    private void requireStatus(Booking booking, BookingStatus required) {
        if (booking.getStatus() != required) {
            throw new BusinessRuleException("Booking must be in " + required + " status, current: " + booking.getStatus());
        }
    }

    private Booking findAndVerify(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        String role = SecurityUtils.getCurrentRole();
        UUID userId = SecurityUtils.getCurrentUserId();

        if (!booking.getTenant().getId().equals(tenantId)) {
            throw new UnauthorizedException("Access denied");
        }
        // Employees can only see their own bookings
        if ("ROLE_EMPLOYEE".equals(role) && !booking.getEmployee().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
        return booking;
    }

    private User currentUser() {
        return userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void appendHistory(Booking booking, BookingStatus from, BookingStatus to, User actor, String reason) {
        BookingStatusHistory history = BookingStatusHistory.builder()
                .tenant(booking.getTenant())
                .booking(booking)
                .fromStatus(from)
                .toStatus(to)
                .actor(actor)
                .reason(reason)
                .transitionedAt(Instant.now())
                .build();
        historyRepository.save(history);
    }

    private BigDecimalHolder estimateFare(Tenant tenant, VehicleType type) {
        return pricingConfigRepository.findByTenant(tenant)
                .map(config -> {
                    java.math.BigDecimal multiplier = switch (type) {
                        case SEDAN -> config.getSedanMultiplier();
                        case SUV -> config.getSuvMultiplier();
                        case LUXURY -> config.getLuxuryMultiplier();
                    };
                    java.math.BigDecimal base = config.getBaseFare().multiply(multiplier);
                    return new BigDecimalHolder(base.max(config.getMinimumFare()));
                })
                .orElse(new BigDecimalHolder(null));
    }

    private java.math.BigDecimal calculateFinalFare(Booking booking) {
        if (booking.getTripStartedAt() == null || booking.getTripCompletedAt() == null) {
            return booking.getEstimatedFare();
        }
        return pricingConfigRepository.findByTenant(booking.getTenant())
                .map(config -> {
                    long minutes = java.time.Duration.between(
                            booking.getTripStartedAt(), booking.getTripCompletedAt()).toMinutes();
                    java.math.BigDecimal hours = new java.math.BigDecimal(minutes)
                            .divide(new java.math.BigDecimal("60"), 4, java.math.RoundingMode.HALF_UP);
                    java.math.BigDecimal multiplier = switch (booking.getVehicleTypeRequested()) {
                        case SEDAN -> config.getSedanMultiplier();
                        case SUV -> config.getSuvMultiplier();
                        case LUXURY -> config.getLuxuryMultiplier();
                    };
                    java.math.BigDecimal fare = config.getBaseFare()
                            .add(config.getPerHourRate().multiply(hours))
                            .multiply(multiplier);
                    return fare.max(config.getMinimumFare()).setScale(2, java.math.RoundingMode.HALF_UP);
                })
                .orElse(booking.getEstimatedFare());
    }

    private record BigDecimalHolder(java.math.BigDecimal value) {}

    private BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .tenantId(b.getTenant().getId())
                .corporateClientId(b.getCorporateClient().getId())
                .corporateClientName(b.getCorporateClient().getCompanyName())
                .employeeUserId(b.getEmployee().getId())
                .employeeName(b.getEmployee().getFullName())
                .driverId(b.getDriver() != null ? b.getDriver().getId() : null)
                .driverName(b.getDriver() != null ? b.getDriver().getUser().getFullName() : null)
                .vehicleId(b.getVehicle() != null ? b.getVehicle().getId() : null)
                .vehiclePlate(b.getVehicle() != null ? b.getVehicle().getPlateNumber() : null)
                .assignmentMode(b.getAssignmentMode())
                .pickupAddress(b.getPickupAddress())
                .dropAddress(b.getDropAddress())
                .pickupLat(b.getPickupLat())
                .pickupLng(b.getPickupLng())
                .dropLat(b.getDropLat())
                .dropLng(b.getDropLng())
                .vehicleTypeRequested(b.getVehicleTypeRequested())
                .scheduledAt(b.getScheduledAt())
                .notes(b.getNotes())
                .status(b.getStatus())
                .cancellationReason(b.getCancellationReason())
                .rejectionReason(b.getRejectionReason())
                .estimatedFare(b.getEstimatedFare())
                .finalFare(b.getFinalFare())
                .cancellationFee(b.getCancellationFee())
                .approvedAt(b.getApprovedAt())
                .driverAssignedAt(b.getDriverAssignedAt())
                .tripStartedAt(b.getTripStartedAt())
                .tripCompletedAt(b.getTripCompletedAt())
                .cancelledAt(b.getCancelledAt())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .driverCurrentLat(b.getDriverCurrentLat())
                .driverCurrentLng(b.getDriverCurrentLng())
                .locationUpdatedAt(b.getLocationUpdatedAt())
                .otpVerifiedAt(b.getOtpVerifiedAt())
                .build();
    }

    private BookingStatusHistoryResponse toHistoryResponse(BookingStatusHistory h) {
        return BookingStatusHistoryResponse.builder()
                .id(h.getId())
                .fromStatus(h.getFromStatus())
                .toStatus(h.getToStatus())
                .actorUserId(h.getActor() != null ? h.getActor().getId() : null)
                .reason(h.getReason())
                .transitionedAt(h.getTransitionedAt())
                .build();
    }
}
