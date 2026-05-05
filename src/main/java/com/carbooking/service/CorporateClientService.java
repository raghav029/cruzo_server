package com.carbooking.service;

import com.carbooking.common.enums.Role;
import com.carbooking.common.enums.UserStatus;
import com.carbooking.common.exception.DuplicateResourceException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.request.corporateclient.CreateCorporateAdminRequest;
import com.carbooking.dto.request.corporateclient.CreateCorporateClientRequest;
import com.carbooking.dto.request.corporateclient.UpdateCorporateClientRequest;
import com.carbooking.dto.response.corporateclient.CorporateAdminResponse;
import com.carbooking.dto.response.corporateclient.CorporateClientResponse;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.repository.CorporateClientRepository;
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
public class CorporateClientService {

    private final CorporateClientRepository clientRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public CorporateClientService(CorporateClientRepository clientRepository,
                                   TenantRepository tenantRepository,
                                   UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   @Lazy NotificationService notificationService) {
        this.clientRepository = clientRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Transactional
    public CorporateClientResponse create(CreateCorporateClientRequest request) {
        Tenant tenant = currentTenant();

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

        return toResponse(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public Page<CorporateClientResponse> list(Pageable pageable) {
        Tenant tenant = currentTenant();
        return clientRepository.findByTenant(tenant, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CorporateClientResponse get(UUID clientId) {
        return toResponse(findAndVerify(clientId));
    }

    @Transactional
    public CorporateClientResponse update(UUID clientId, UpdateCorporateClientRequest request) {
        CorporateClient client = findAndVerify(clientId);

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

        return toResponse(clientRepository.save(client));
    }

    @Transactional
    public void delete(UUID clientId) {
        CorporateClient client = findAndVerify(clientId);
        client.setActive(false);
        clientRepository.save(client);
    }

    @Transactional
    public CorporateAdminResponse createAdmin(UUID clientId, CreateCorporateAdminRequest request) {
        CorporateClient client = findAndVerify(clientId);

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

    // ── helpers ──────────────────────────────────────────────────────────────

    private CorporateClient findAndVerify(UUID clientId) {
        CorporateClient client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found: " + clientId));
        if (!client.getTenant().getId().equals(SecurityUtils.getCurrentTenantId())) {
            throw new UnauthorizedException("Access denied");
        }
        return client;
    }

    private Tenant currentTenant() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    private CorporateClientResponse toResponse(CorporateClient c) {
        return CorporateClientResponse.builder()
                .id(c.getId())
                .tenantId(c.getTenant().getId())
                .companyName(c.getCompanyName())
                .gstNumber(c.getGstNumber())
                .billingAddress(c.getBillingAddress())
                .billingEmail(c.getBillingEmail())
                .billingCycle(c.getBillingCycle())
                .creditLimit(c.getCreditLimit())
                .currentOutstanding(c.getCurrentOutstanding())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
