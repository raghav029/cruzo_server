package com.carbooking.modules.client.application;

import com.carbooking.common.enums.Role;
import com.carbooking.common.enums.UserStatus;
import com.carbooking.common.exception.DuplicateResourceException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.modules.client.dto.request.CreateCorporateAdminRequest;
import com.carbooking.modules.client.dto.request.CreateCorporateClientRequest;
import com.carbooking.modules.client.dto.request.UpdateCorporateClientRequest;
import com.carbooking.modules.client.dto.response.CorporateAdminResponse;
import com.carbooking.modules.client.dto.response.CorporateClientResponse;
import com.carbooking.modules.client.domain.port.CorporateClientPort;
import com.carbooking.modules.notification.application.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carbooking.common.enums.VehicleType;
import com.carbooking.common.exception.BusinessRuleException;
import java.util.Arrays;
import java.util.UUID;
import com.carbooking.modules.client.application.CorporateClientMapper;

@Slf4j
@Service
public class CorporateClientService extends TenantSupport {

    private final CorporateClientMapper clientMapper;
    private final CorporateClientPort clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public CorporateClientService(CorporateClientPort clientRepository,
                                   PasswordEncoder passwordEncoder,
                                   @Lazy NotificationService notificationService,
                                 CorporateClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.clientMapper = clientMapper;
    }

    @Transactional
    public CorporateClientResponse create(CreateCorporateClientRequest request) {
        Tenant tenant = requireTenant();

        if (clientRepository.existsByTenantAndCompanyName(tenant, request.getCompanyName())) {
            throw new DuplicateResourceException("Corporate client '" + request.getCompanyName() + "' already exists");
        }

        CorporateClient client = CorporateClient.builder()
                .tenant(tenant)
                .companyName(request.getCompanyName())
                .gstNumber(request.getGstNumber())
                .billingAddress(request.getBillingAddress())
                .billingEmail(request.getBillingEmail())
                .billingCycle(request.getBillingCycle())
                .creditLimit(request.getCreditLimit() != null ? request.getCreditLimit() : java.math.BigDecimal.ZERO)
                .currentOutstanding(java.math.BigDecimal.ZERO)
                .active(true)
                .build();

        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public Page<CorporateClientResponse> list(Pageable pageable) {
        Tenant tenant = requireTenant();
        return clientRepository.findByTenant(tenant, pageable).map(clientMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CorporateClientResponse get(UUID clientId) {
        CorporateClient client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found: " + clientId));
        assertSameTenant(client.getTenant().getId());
        return clientMapper.toResponse(client);
    }

    @Transactional
    public CorporateClientResponse update(UUID clientId, UpdateCorporateClientRequest request) {
        CorporateClient client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found: " + clientId));
        assertSameTenant(client.getTenant().getId());

        if (request.getCompanyName() != null) {
            if (!request.getCompanyName().equals(client.getCompanyName()) &&
                    clientRepository.existsByTenantAndCompanyName(client.getTenant(), request.getCompanyName())) {
                throw new DuplicateResourceException("Corporate client '" + request.getCompanyName() + "' already exists");
            }
            client.setCompanyName(request.getCompanyName());
        }
        if (request.getGstNumber() != null) client.setGstNumber(request.getGstNumber());
        if (request.getBillingAddress() != null) client.setBillingAddress(request.getBillingAddress());
        if (request.getBillingEmail() != null) client.setBillingEmail(request.getBillingEmail());
        if (request.getBillingCycle() != null) client.setBillingCycle(request.getBillingCycle());
        if (request.getCreditLimit() != null) client.setCreditLimit(request.getCreditLimit());
        if (request.getActive() != null) client.setActive(request.getActive());
        if (request.getMaxBookingValue() != null) client.setMaxBookingValue(request.getMaxBookingValue());
        if (request.getAllowedVehicleTypes() != null) {
            validateAllowedVehicleTypes(request.getAllowedVehicleTypes());
            client.setAllowedVehicleTypes(request.getAllowedVehicleTypes().isBlank() ? null : request.getAllowedVehicleTypes().trim());
        }

        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Transactional
    public void delete(UUID clientId) {
        CorporateClient client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found: " + clientId));
        assertSameTenant(client.getTenant().getId());
        client.setActive(false);
        clientRepository.save(client);
    }

    @Transactional
    public CorporateAdminResponse createAdmin(UUID clientId, CreateCorporateAdminRequest request) {
        CorporateClient client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found: " + clientId));
        assertSameTenant(client.getTenant().getId());

        if (userRepository.existsByEmailAndTenant(request.getEmail(), client.getTenant())) {
            throw new DuplicateResourceException("User with email '" + request.getEmail() + "' already exists");
        }

        String tempPassword = "Adm@" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Temp password for corporate admin {}: {}", request.getEmail(), tempPassword);

        User admin = User.builder()
                .tenant(client.getTenant())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(Role.CORPORATE_ADMIN)
                .status(UserStatus.ACTIVE)
                .corporateClient(client)
                .build();
        userRepository.save(admin);
        notificationService.sendTempPassword(admin, tempPassword);

        return CorporateAdminResponse.builder()
                .userId(admin.getId())
                .tenantId(client.getTenant().getId())
                .corporateClientId(client.getId())
                .fullName(admin.getFullName())
                .email(admin.getEmail())
                .phone(admin.getPhone())
                .createdAt(admin.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public CorporateClientResponse getMy() {
        User user = currentUser();
        CorporateClient client = user.getCorporateClient();
        if (client == null) throw new ResourceNotFoundException("No corporate client linked to this user");
        return clientMapper.toResponse(client);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void validateAllowedVehicleTypes(String value) {
        if (value == null || value.isBlank()) return;
        Arrays.stream(value.split(","))
            .map(String::trim)
            .forEach(v -> {
                try {
                    VehicleType.valueOf(v);
                } catch (IllegalArgumentException e) {
                    throw new BusinessRuleException("Invalid vehicle type: " + v + ". Allowed: SEDAN, SUV, LUXURY");
                }
            });
    }

}
