package com.carbooking.service;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.enums.DriverAvailability;
import com.carbooking.common.enums.Role;
import com.carbooking.common.enums.UserStatus;
import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.DuplicateResourceException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.dto.request.tenant.CreateTenantRequest;
import com.carbooking.dto.request.tenant.UpdateTenantRequest;
import com.carbooking.dto.response.tenant.TenantResponse;
import com.carbooking.dto.response.tenant.TenantStatsResponse;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.repository.BookingRepository;
import com.carbooking.repository.CorporateEmployeeRepository;
import com.carbooking.repository.DriverRepository;
import com.carbooking.repository.TenantRepository;
import com.carbooking.repository.UserRepository;
import com.carbooking.repository.VehicleRepository;
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
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final CorporateEmployeeRepository corporateEmployeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public TenantService(TenantRepository tenantRepository, UserRepository userRepository,
                         BookingRepository bookingRepository, DriverRepository driverRepository,
                         VehicleRepository vehicleRepository,
                         CorporateEmployeeRepository corporateEmployeeRepository,
                         PasswordEncoder passwordEncoder,
                         @Lazy NotificationService notificationService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.corporateEmployeeRepository = corporateEmployeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Transactional
    public TenantResponse create(CreateTenantRequest request) {
        if (tenantRepository.existsBySubdomain(request.getSubdomain())) {
            throw new DuplicateResourceException("Subdomain '" + request.getSubdomain() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getFleetManagerEmail())) {
            throw new DuplicateResourceException("Email '" + request.getFleetManagerEmail() + "' is already registered");
        }

        Tenant tenant = Tenant.builder()
                .name(request.getName())
                .subdomain(request.getSubdomain())
                .supportEmail(request.getSupportEmail())
                .supportPhone(request.getSupportPhone())
                .logoUrl(request.getLogoUrl())
                .primaryColor(request.getPrimaryColor())
                .secondaryColor(request.getSecondaryColor())
                .active(true)
                .build();

        tenant = tenantRepository.save(tenant);

        // Auto-create the FLEET_MANAGER user for this tenant
        String tempPassword = generateTempPassword();
        User fleetManager = User.builder()
                .tenant(tenant)
                .fullName(request.getFleetManagerName())
                .email(request.getFleetManagerEmail())
                .phone(request.getFleetManagerPhone())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(Role.FLEET_MANAGER)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(fleetManager);

        log.info("Created tenant [{}] with fleet manager [{}]. Temp password: {}",
                tenant.getId(), fleetManager.getEmail(), tempPassword);
        notificationService.sendTempPassword(fleetManager, tempPassword);

        return toResponse(tenant);
    }

    @Transactional(readOnly = true)
    public Page<TenantResponse> list(Pageable pageable) {
        return tenantRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TenantResponse get(UUID tenantId) {
        return toResponse(findOrThrow(tenantId));
    }

    @Transactional
    public TenantResponse update(UUID tenantId, UpdateTenantRequest request) {
        Tenant tenant = findOrThrow(tenantId);

        if (request.getName() != null) tenant.setName(request.getName());
        if (request.getSupportEmail() != null) tenant.setSupportEmail(request.getSupportEmail());
        if (request.getSupportPhone() != null) tenant.setSupportPhone(request.getSupportPhone());
        if (request.getLogoUrl() != null) tenant.setLogoUrl(request.getLogoUrl());
        if (request.getPrimaryColor() != null) tenant.setPrimaryColor(request.getPrimaryColor());
        if (request.getSecondaryColor() != null) tenant.setSecondaryColor(request.getSecondaryColor());
        if (request.getActive() != null) tenant.setActive(request.getActive());

        return toResponse(tenantRepository.save(tenant));
    }

    @Transactional
    public void delete(UUID tenantId) {
        Tenant tenant = findOrThrow(tenantId);

        long activeBookings = bookingRepository.countActiveByTenant(tenant);
        if (activeBookings > 0) {
            throw new BusinessRuleException(
                "Cannot deactivate tenant with " + activeBookings + " active booking(s). Resolve them first.");
        }

        tenant.setActive(false);
        tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public TenantStatsResponse getStats(UUID tenantId) {
        Tenant tenant = findOrThrow(tenantId);

        return TenantStatsResponse.builder()
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .totalBookings(bookingRepository.countByTenant(tenant))
                .activeBookings(bookingRepository.countActiveByTenant(tenant))
                .totalDrivers(driverRepository.countByTenant(tenant))
                .availableDrivers(driverRepository.countByTenantAndAvailability(tenant, DriverAvailability.AVAILABLE))
                .totalVehicles(vehicleRepository.countByTenant(tenant))
                .activeVehicles(vehicleRepository.countByTenantAndStatus(tenant, VehicleStatus.ACTIVE))
                .totalEmployees(userRepository.countByTenantAndRole(tenant, Role.EMPLOYEE))
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Tenant findOrThrow(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));
    }

    private TenantResponse toResponse(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .subdomain(tenant.getSubdomain())
                .supportEmail(tenant.getSupportEmail())
                .supportPhone(tenant.getSupportPhone())
                .logoUrl(tenant.getLogoUrl())
                .primaryColor(tenant.getPrimaryColor())
                .secondaryColor(tenant.getSecondaryColor())
                .active(tenant.isActive())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }

    private String generateTempPassword() {
        // 8-char alphanumeric temp password — in production replace with email delivery
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
