package com.carbooking.modules.recurring.infrastructure.persistence;

import com.carbooking.entity.RecurringBooking;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.modules.recurring.domain.port.RecurringBookingPort;
import com.carbooking.repository.RecurringBookingRepository;
import com.carbooking.modules.recurring.domain.port.RecurringBookingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RecurringBookingJpaAdapter implements RecurringBookingPort {

    private final RecurringBookingRepository repo;

    @Override public Optional<RecurringBooking> findById(UUID id) { return repo.findById(id); }
    @Override public List<RecurringBooking> findByTenantAndIsActiveTrue(Tenant tenant) { return repo.findByTenantAndIsActiveTrue(tenant); }
    @Override public List<RecurringBooking> findByTenantAndEmployeeAndIsActiveTrue(Tenant tenant, User employee) { return repo.findByTenantAndEmployeeAndIsActiveTrue(tenant, employee); }
    @Override public RecurringBooking save(RecurringBooking rb) { return repo.save(rb); }
}
