package com.carbooking.service;

import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.request.recurring.CreateRecurringBookingRequest;
import com.carbooking.dto.response.recurring.RecurringBookingResponse;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.RecurringBooking;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.repository.CorporateClientRepository;
import com.carbooking.repository.RecurringBookingRepository;
import com.carbooking.repository.TenantRepository;
import com.carbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecurringBookingService {

    private final RecurringBookingRepository recurringBookingRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final CorporateClientRepository corporateClientRepository;

    @Transactional
    public RecurringBookingResponse create(CreateRecurringBookingRequest request) {
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

        RecurringBooking rb = RecurringBooking.builder()
                .tenant(tenant)
                .corporateClient(client)
                .employee(employee)
                .pickupAddress(request.getPickupAddress())
                .dropAddress(request.getDropAddress())
                .vehicleType(request.getVehicleType())
                .scheduledTime(request.getScheduledTime())
                .recurrenceDays(String.join(",", request.getRecurrenceDays()))
                .isActive(true)
                .notes(request.getNotes())
                .build();

        return toResponse(recurringBookingRepository.save(rb));
    }

    @Transactional(readOnly = true)
    public List<RecurringBookingResponse> list() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        List<RecurringBooking> results;
        if ("ROLE_FLEET_MANAGER".equals(role)) {
            results = recurringBookingRepository.findByTenantAndIsActiveTrue(tenant);
        } else {
            User employee = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            results = recurringBookingRepository.findByTenantAndEmployeeAndIsActiveTrue(tenant, employee);
        }

        return results.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecurringBookingResponse get(UUID id) {
        return toResponse(findAndVerify(id));
    }

    @Transactional
    public RecurringBookingResponse deactivate(UUID id) {
        RecurringBooking rb = findAndVerify(id);
        rb.setActive(false);
        return toResponse(recurringBookingRepository.save(rb));
    }

    private RecurringBooking findAndVerify(UUID id) {
        RecurringBooking rb = recurringBookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring booking not found: " + id));
        if (!rb.getTenant().getId().equals(SecurityUtils.getCurrentTenantId())) {
            throw new UnauthorizedException("Access denied");
        }
        return rb;
    }

    private RecurringBookingResponse toResponse(RecurringBooking rb) {
        return RecurringBookingResponse.builder()
                .id(rb.getId())
                .tenantId(rb.getTenant().getId())
                .corporateClientId(rb.getCorporateClient().getId())
                .employeeUserId(rb.getEmployee().getId())
                .employeeName(rb.getEmployee().getFullName())
                .pickupAddress(rb.getPickupAddress())
                .dropAddress(rb.getDropAddress())
                .vehicleType(rb.getVehicleType())
                .scheduledTime(rb.getScheduledTime())
                .recurrenceDays(Arrays.asList(rb.getRecurrenceDays().split(",")))
                .isActive(rb.isActive())
                .notes(rb.getNotes())
                .createdAt(rb.getCreatedAt())
                .build();
    }
}
