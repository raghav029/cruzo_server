package com.carbooking.service;

import com.carbooking.common.enums.DriverAvailability;
import com.carbooking.common.enums.Role;
import com.carbooking.common.enums.UserStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.DuplicateResourceException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.request.driver.CreateDriverRequest;
import com.carbooking.dto.request.driver.UpdateDriverRequest;
import com.carbooking.dto.response.driver.DriverResponse;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.repository.BookingRepository;
import com.carbooking.repository.DriverRepository;
import com.carbooking.repository.TenantRepository;
import com.carbooking.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public DriverService(DriverRepository driverRepository, UserRepository userRepository,
                         TenantRepository tenantRepository, BookingRepository bookingRepository,
                         PasswordEncoder passwordEncoder,
                         @Lazy NotificationService notificationService) {
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.bookingRepository = bookingRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Transactional
    public DriverResponse create(CreateDriverRequest request) {
        Tenant tenant = currentTenant();

        if (userRepository.existsByEmailAndTenant(request.getEmail(), tenant)) {
            throw new DuplicateResourceException("User with email '" + request.getEmail() + "' already exists");
        }
        if (driverRepository.existsByTenantAndLicenseNumber(tenant, request.getLicenseNumber())) {
            throw new DuplicateResourceException("Driver with license '" + request.getLicenseNumber() + "' already exists");
        }

        String tempPassword = generateTempPassword();
        log.info("Temp password for driver {}: {}", request.getEmail(), tempPassword);
        // SMS sent after user is saved below

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

        DriverResponse response = toResponse(driverRepository.save(driver));
        notificationService.sendTempPassword(user, tempPassword);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<DriverResponse> list(DriverAvailability availability, Pageable pageable) {
        Tenant tenant = currentTenant();
        Page<Driver> page = (availability != null)
                ? driverRepository.findByTenantAndAvailability(tenant, availability, pageable)
                : driverRepository.findByTenant(tenant, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DriverResponse get(UUID driverId) {
        return toResponse(findAndVerify(driverId));
    }

    @Transactional
    public DriverResponse update(UUID driverId, UpdateDriverRequest request) {
        Driver driver = findAndVerify(driverId);

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

        return toResponse(driverRepository.save(driver));
    }

    @Transactional
    public void delete(UUID driverId) {
        Driver driver = findAndVerify(driverId);

        if (driver.getAvailability() == DriverAvailability.ON_TRIP) {
            throw new BusinessRuleException("Cannot deactivate driver who is ON_TRIP");
        }

        boolean hasActiveBooking = bookingRepository
                .findByTenant(driver.getTenant(), Pageable.unpaged())
                .stream()
                .anyMatch(b -> b.getDriver() != null
                        && b.getDriver().getId().equals(driverId)
                        && isActiveStatus(b.getStatus().name()));

        if (hasActiveBooking) {
            throw new BusinessRuleException("Cannot deactivate driver with an active booking");
        }

        User user = driver.getUser();
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Driver findAndVerify(UUID driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + driverId));
        if (!driver.getTenant().getId().equals(SecurityUtils.getCurrentTenantId())) {
            throw new UnauthorizedException("Access denied");
        }
        return driver;
    }

    private Tenant currentTenant() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    private boolean isActiveStatus(String status) {
        return switch (status) {
            case "PENDING_APPROVAL", "APPROVED", "DRIVER_ASSIGNED",
                 "DRIVER_EN_ROUTE", "ARRIVED", "IN_PROGRESS" -> true;
            default -> false;
        };
    }

    private String generateTempPassword() {
        return "Drv@" + UUID.randomUUID().toString().substring(0, 8);
    }

    private DriverResponse toResponse(Driver d) {
        return DriverResponse.builder()
                .id(d.getId())
                .tenantId(d.getTenant().getId())
                .userId(d.getUser().getId())
                .fullName(d.getUser().getFullName())
                .email(d.getUser().getEmail())
                .phone(d.getUser().getPhone())
                .licenseNumber(d.getLicenseNumber())
                .licenseExpiry(d.getLicenseExpiry())
                .insuranceExpiry(d.getInsuranceExpiry())
                .availability(d.getAvailability())
                .currentVehicleId(d.getCurrentVehicle() != null ? d.getCurrentVehicle().getId() : null)
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
