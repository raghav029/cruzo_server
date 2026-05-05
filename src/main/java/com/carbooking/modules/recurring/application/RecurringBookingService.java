package com.carbooking.modules.recurring.application;

import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.RecurringBooking;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.modules.recurring.dto.request.CreateRecurringBookingRequest;
import com.carbooking.modules.recurring.dto.response.RecurringBookingResponse;
import com.carbooking.modules.client.domain.port.CorporateClientPort;
import com.carbooking.modules.recurring.domain.port.RecurringBookingPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.carbooking.modules.recurring.application.RecurringBookingMapper;

@Service
public class RecurringBookingService extends TenantSupport {

    private final RecurringBookingMapper recurringMapper;
    private final RecurringBookingPort recurringBookingRepository;
    private final CorporateClientPort corporateClientRepository;

    public RecurringBookingService(RecurringBookingPort recurringBookingRepository,
                                   CorporateClientPort corporateClientRepository,
                                 RecurringBookingMapper recurringMapper) {
        this.recurringBookingRepository = recurringBookingRepository;
        this.corporateClientRepository = corporateClientRepository;
        this.recurringMapper = recurringMapper;
    }

    @Transactional
    public RecurringBookingResponse create(CreateRecurringBookingRequest request) {
        Tenant tenant = requireTenant();
        User employee = currentUser();

        CorporateClient client = corporateClientRepository.findById(request.getCorporateClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found"));
        assertSameTenant(client.getTenant().getId());

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

        return recurringMapper.toResponse(recurringBookingRepository.save(rb));
    }

    @Transactional(readOnly = true)
    public List<RecurringBookingResponse> list() {
        Tenant tenant = requireTenant();
        String role = currentRole();

        List<RecurringBooking> results;
        if ("ROLE_FLEET_MANAGER".equals(role)) {
            results = recurringBookingRepository.findByTenantAndIsActiveTrue(tenant);
        } else {
            User employee = currentUser();
            results = recurringBookingRepository.findByTenantAndEmployeeAndIsActiveTrue(tenant, employee);
        }

        return results.stream().map(recurringMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecurringBookingResponse get(UUID id) {
        return recurringMapper.toResponse(findRecurring(id));
    }

    @Transactional
    public RecurringBookingResponse deactivate(UUID id) {
        RecurringBooking rb = findRecurring(id);
        rb.setActive(false);
        return recurringMapper.toResponse(recurringBookingRepository.save(rb));
    }

    private RecurringBooking findRecurring(UUID id) {
        RecurringBooking rb = recurringBookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring booking not found: " + id));
        assertSameTenant(rb.getTenant().getId());
        return rb;
    }

}
