package com.carbooking.modules.fleet.application;

import com.carbooking.common.enums.DriverAvailability;
import com.carbooking.common.enums.Role;
import com.carbooking.common.enums.UserStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.DuplicateResourceException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.modules.fleet.dto.request.CreateDriverRequest;
import com.carbooking.modules.fleet.dto.request.UpdateDriverRequest;
import com.carbooking.modules.fleet.dto.response.DriverResponse;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.fleet.domain.port.DriverPort;
import com.carbooking.modules.notification.application.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import com.carbooking.modules.fleet.application.DriverMapper;

@Slf4j
@Service
public class DriverService extends TenantSupport {

    private final DriverMapper driverMapper;
    private final DriverPort driverRepository;
    private final BookingPort bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public DriverService(DriverPort driverRepository, BookingPort bookingRepository,
                         PasswordEncoder passwordEncoder,
                         @Lazy NotificationService notificationService,
                                 DriverMapper driverMapper) {
        this.driverRepository = driverRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.driverMapper = driverMapper;
    }

    @Transactional
    public DriverResponse create(CreateDriverRequest request) {
        Tenant tenant = requireTenant();

        if (userRepository.existsByEmailAndTenant(request.getEmail(), tenant)) {
            throw new DuplicateResourceException("User with email '" + request.getEmail() + "' already exists");
        }
        if (driverRepository.existsByTenantAndLicenseNumber(tenant, request.getLicenseNumber())) {
            throw new DuplicateResourceException("Driver with license '" + request.getLicenseNumber() + "' already exists");
        }

        String tempPassword = generateTempPassword();
        log.info("Temp password for driver {}: {}", request.getEmail(), tempPassword);

        User user = User.builder()
                .tenant(tenant)
                .fullName(request.getFirstName() + " " + request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(Role.DRIVER)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        Driver driver = Driver.builder()
                .tenant(tenant)
                .user(user)
                .licenseNumber(request.getLicenseNumber())
                .licenseExpiry(request.getLicenseExpiry())
                .insuranceExpiry(request.getInsuranceExpiry())
                .availability(DriverAvailability.OFF_DUTY)
                .build();

        DriverResponse response = driverMapper.toResponse(driverRepository.save(driver));
        notificationService.sendTempPassword(user, tempPassword);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<DriverResponse> list(DriverAvailability availability, Pageable pageable) {
        Tenant tenant = requireTenant();
        Page<Driver> page = (availability != null)
                ? driverRepository.findByTenantAndAvailability(tenant, availability, pageable)
                : driverRepository.findByTenant(tenant, pageable);
        return page.map(driverMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public DriverResponse get(UUID driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + driverId));
        assertSameTenant(driver.getTenant().getId());
        return driverMapper.toResponse(driver);
    }

    @Transactional
    public DriverResponse update(UUID driverId, UpdateDriverRequest request) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + driverId));
        assertSameTenant(driver.getTenant().getId());

        if (request.getAvailability() != null) {
            if (driver.getAvailability() == DriverAvailability.ON_TRIP) {
                throw new BusinessRuleException("Cannot change availability of a driver who is ON_TRIP");
            }
            driver.setAvailability(request.getAvailability());
        }
        if (request.getLicenseNumber() != null) {
            if (!request.getLicenseNumber().equals(driver.getLicenseNumber()) &&
                    driverRepository.existsByTenantAndLicenseNumber(driver.getTenant(), request.getLicenseNumber())) {
                throw new DuplicateResourceException("License number already in use");
            }
            driver.setLicenseNumber(request.getLicenseNumber());
        }
        if (request.getLicenseExpiry() != null) driver.setLicenseExpiry(request.getLicenseExpiry());
        if (request.getInsuranceExpiry() != null) driver.setInsuranceExpiry(request.getInsuranceExpiry());
        if (request.getPhone() != null) driver.getUser().setPhone(request.getPhone());

        return driverMapper.toResponse(driverRepository.save(driver));
    }

    @Transactional
    public void delete(UUID driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + driverId));
        assertSameTenant(driver.getTenant().getId());

        if (driver.getAvailability() == DriverAvailability.ON_TRIP) {
            throw new BusinessRuleException("Cannot deactivate driver who is ON_TRIP");
        }

        boolean hasActiveBooking = bookingRepository
                .findByTenant(driver.getTenant(), Pageable.unpaged())
                .stream()
                .anyMatch(b -> b.getDriver() != null
                        && b.getDriver().getId().equals(driverId)
                        && isActiveBookingStatus(b.getStatus().name()));

        if (hasActiveBooking) {
            throw new BusinessRuleException("Cannot deactivate driver with an active booking");
        }

        User user = driver.getUser();
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private boolean isActiveBookingStatus(String status) {
        return switch (status) {
            case "PENDING_APPROVAL", "APPROVED", "DRIVER_ASSIGNED",
                 "DRIVER_EN_ROUTE", "ARRIVED", "IN_PROGRESS" -> true;
            default -> false;
        };
    }

    private String generateTempPassword() {
        return "Drv@" + UUID.randomUUID().toString().substring(0, 8);
    }

}
