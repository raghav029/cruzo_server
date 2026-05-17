package com.carbooking.modules.client.application;

import com.carbooking.common.enums.Role;
import com.carbooking.common.enums.UserStatus;
import com.carbooking.common.exception.DuplicateResourceException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.CorporateEmployee;
import com.carbooking.entity.User;
import com.carbooking.modules.client.dto.request.CreateCorporateEmployeeRequest;
import com.carbooking.modules.client.dto.request.UpdateCorporateEmployeeRequest;
import com.carbooking.modules.client.dto.request.UpdateEmployeeSelfRequest;
import com.carbooking.modules.client.dto.response.CorporateEmployeeResponse;
import com.carbooking.modules.client.domain.port.CorporateClientPort;
import com.carbooking.modules.client.domain.port.CorporateEmployeePort;
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
import com.carbooking.modules.client.dto.response.TravelPolicyResponse;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;
import com.carbooking.modules.client.application.CorporateEmployeeMapper;

@Slf4j
@Service
public class CorporateEmployeeService extends TenantSupport {

    private final CorporateEmployeeMapper employeeMapper;
    private final CorporateEmployeePort employeeRepository;
    private final CorporateClientPort clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public CorporateEmployeeService(CorporateEmployeePort employeeRepository,
                                     CorporateClientPort clientRepository,
                                     PasswordEncoder passwordEncoder,
                                     @Lazy NotificationService notificationService,
                                 CorporateEmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.employeeMapper = employeeMapper;
    }

    @Transactional
    public CorporateEmployeeResponse create(UUID clientId, CreateCorporateEmployeeRequest request) {
        CorporateClient client = findClient(clientId);

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

        validateAllowedVehicleTypes(request.getAllowedVehicleTypesOverride());

        CorporateEmployee employee = CorporateEmployee.builder()
                .tenant(client.getTenant())
                .corporateClient(client)
                .user(user)
                .employeeCode(request.getEmployeeCode())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .monthlyRideLimit(request.getMonthlyRideLimit())
                .maxBookingValueOverride(request.getMaxBookingValueOverride())
                .allowedVehicleTypesOverride(request.getAllowedVehicleTypesOverride())
                .active(true)
                .build();

        CorporateEmployeeResponse response = employeeMapper.toResponse(employeeRepository.save(employee));
        notificationService.sendTempPassword(user, tempPassword);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<CorporateEmployeeResponse> list(UUID clientId, Pageable pageable) {
        CorporateClient client = findClient(clientId);
        return employeeRepository.findByCorporateClient(client, pageable).map(employeeMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CorporateEmployeeResponse get(UUID clientId, UUID employeeId) {
        findClient(clientId);
        CorporateEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        assertSameTenant(employee.getTenant().getId());
        return employeeMapper.toResponse(employee);
    }

    @Transactional
    public CorporateEmployeeResponse update(UUID clientId, UUID employeeId,
                                             UpdateCorporateEmployeeRequest request) {
        findClient(clientId);
        CorporateEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        assertSameTenant(employee.getTenant().getId());

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
        if (request.getMaxBookingValueOverride() != null) employee.setMaxBookingValueOverride(request.getMaxBookingValueOverride());
        if (request.getAllowedVehicleTypesOverride() != null) {
            validateAllowedVehicleTypes(request.getAllowedVehicleTypesOverride());
            employee.setAllowedVehicleTypesOverride(request.getAllowedVehicleTypesOverride().isBlank() ? null : request.getAllowedVehicleTypesOverride().trim());
        }

        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public void delete(UUID clientId, UUID employeeId) {
        findClient(clientId);
        CorporateEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        assertSameTenant(employee.getTenant().getId());

        employee.setActive(false);
        User user = employee.getUser();
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        employeeRepository.save(employee);
    }

    @Transactional(readOnly = true)
    public CorporateEmployeeResponse getMe() {
        User currentUser = currentUser();
        CorporateEmployee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found for current user"));
        return employeeMapper.toResponse(employee);
    }

    @Transactional
    public CorporateEmployeeResponse updateMe(UpdateEmployeeSelfRequest request) {
        User currentUser = currentUser();
        CorporateEmployee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found for current user"));
        if (request.getPhone() != null) employee.getUser().setPhone(request.getPhone());
        if (request.getDepartment() != null) employee.setDepartment(request.getDepartment());
        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public TravelPolicyResponse getMyPolicy() {
        User currentUser = currentUser();
        CorporateEmployee employee = employeeRepository.findByUser(currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found for current user"));

        BigDecimal effectiveMax = employee.getMaxBookingValueOverride() != null
                ? employee.getMaxBookingValueOverride()
                : employee.getCorporateClient().getMaxBookingValue();

        String effectiveTypes = employee.getAllowedVehicleTypesOverride() != null
                ? employee.getAllowedVehicleTypesOverride()
                : employee.getCorporateClient().getAllowedVehicleTypes();

        return TravelPolicyResponse.builder()
                .maxBookingValue(effectiveMax)
                .allowedVehicleTypes(effectiveTypes)
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CorporateClient findClient(UUID clientId) {
        CorporateClient client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found: " + clientId));
        assertSameTenant(client.getTenant().getId());
        return client;
    }

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
