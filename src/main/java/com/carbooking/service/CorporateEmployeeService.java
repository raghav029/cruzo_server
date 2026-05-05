package com.carbooking.service;

import com.carbooking.common.enums.Role;
import com.carbooking.common.enums.UserStatus;
import com.carbooking.common.exception.DuplicateResourceException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.request.corporateemployee.CreateCorporateEmployeeRequest;
import com.carbooking.dto.request.corporateemployee.UpdateCorporateEmployeeRequest;
import com.carbooking.dto.response.corporateemployee.CorporateEmployeeResponse;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.CorporateEmployee;
import com.carbooking.entity.User;
import com.carbooking.repository.CorporateClientRepository;
import com.carbooking.repository.CorporateEmployeeRepository;
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
public class CorporateEmployeeService {

    private final CorporateEmployeeRepository employeeRepository;
    private final CorporateClientRepository clientRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public CorporateEmployeeService(CorporateEmployeeRepository employeeRepository,
                                     CorporateClientRepository clientRepository,
                                     TenantRepository tenantRepository,
                                     UserRepository userRepository,
                                     PasswordEncoder passwordEncoder,
                                     @Lazy NotificationService notificationService) {
        this.employeeRepository = employeeRepository;
        this.clientRepository = clientRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Transactional
    public CorporateEmployeeResponse create(UUID clientId, CreateCorporateEmployeeRequest request) {
        CorporateClient client = findClientAndVerify(clientId);

        if (userRepository.existsByEmailAndTenant(request.getEmail(), client.getTenant())) {
            throw new DuplicateResourceException("User with email '" + request.getEmail() + "' already exists");
        }
        if (request.getEmployeeCode() != null &&
                employeeRepository.existsByCorporateClientAndEmployeeCode(client, request.getEmployeeCode())) {
            throw new DuplicateResourceException("Employee code '" + request.getEmployeeCode() + "' already exists");
        }

        String tempPassword = "Emp@" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Temp password for employee {}: {}", request.getEmail(), tempPassword);

        User user = User.builder()
                .tenant(client.getTenant())
                .fullName(request.getFirstName() + " " + request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(Role.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        CorporateEmployee employee = CorporateEmployee.builder()
                .tenant(client.getTenant())
                .corporateClient(client)
                .user(user)
                .employeeCode(request.getEmployeeCode())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .monthlyRideLimit(request.getMonthlyRideLimit())
                .active(true)
                .build();

        CorporateEmployeeResponse response = toResponse(employeeRepository.save(employee));
        notificationService.sendTempPassword(user, tempPassword);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<CorporateEmployeeResponse> list(UUID clientId, Pageable pageable) {
        CorporateClient client = findClientAndVerify(clientId);
        return employeeRepository.findByCorporateClient(client, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CorporateEmployeeResponse get(UUID clientId, UUID employeeId) {
        findClientAndVerify(clientId);
        return toResponse(findEmployeeAndVerify(employeeId));
    }

    @Transactional
    public CorporateEmployeeResponse update(UUID clientId, UUID employeeId,
                                             UpdateCorporateEmployeeRequest request) {
        findClientAndVerify(clientId);
        CorporateEmployee employee = findEmployeeAndVerify(employeeId);

        if (request.getEmployeeCode() != null) {
            if (!request.getEmployeeCode().equals(employee.getEmployeeCode()) &&
                    employeeRepository.existsByCorporateClientAndEmployeeCode(
                            employee.getCorporateClient(), request.getEmployeeCode())) {
                throw new DuplicateResourceException("Employee code already in use");
            }
            employee.setEmployeeCode(request.getEmployeeCode());
        }
        if (request.getPhone() != null) employee.getUser().setPhone(request.getPhone());
        if (request.getDepartment() != null) employee.setDepartment(request.getDepartment());
        if (request.getDesignation() != null) employee.setDesignation(request.getDesignation());
        if (request.getMonthlyRideLimit() != null) employee.setMonthlyRideLimit(request.getMonthlyRideLimit());
        if (request.getActive() != null) employee.setActive(request.getActive());

        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public void delete(UUID clientId, UUID employeeId) {
        findClientAndVerify(clientId);
        CorporateEmployee employee = findEmployeeAndVerify(employeeId);
        employee.setActive(false);
        User user = employee.getUser();
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        employeeRepository.save(employee);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CorporateClient findClientAndVerify(UUID clientId) {
        CorporateClient client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found: " + clientId));
        if (!client.getTenant().getId().equals(SecurityUtils.getCurrentTenantId())) {
            throw new UnauthorizedException("Access denied");
        }
        return client;
    }

    private CorporateEmployee findEmployeeAndVerify(UUID employeeId) {
        CorporateEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        if (!employee.getTenant().getId().equals(SecurityUtils.getCurrentTenantId())) {
            throw new UnauthorizedException("Access denied");
        }
        return employee;
    }

    private CorporateEmployeeResponse toResponse(CorporateEmployee e) {
        return CorporateEmployeeResponse.builder()
                .id(e.getId())
                .tenantId(e.getTenant().getId())
                .corporateClientId(e.getCorporateClient().getId())
                .userId(e.getUser().getId())
                .fullName(e.getUser().getFullName())
                .email(e.getUser().getEmail())
                .phone(e.getUser().getPhone())
                .employeeCode(e.getEmployeeCode())
                .department(e.getDepartment())
                .designation(e.getDesignation())
                .monthlyRideLimit(e.getMonthlyRideLimit())
                .active(e.isActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
