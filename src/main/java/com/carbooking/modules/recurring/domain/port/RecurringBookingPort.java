package com.carbooking.modules.recurring.domain.port;

import com.carbooking.entity.RecurringBooking;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringBookingPort {
    Optional<RecurringBooking> findById(UUID id);
    List<RecurringBooking> findByTenantAndIsActiveTrue(Tenant tenant);
    List<RecurringBooking> findByTenantAndEmployeeAndIsActiveTrue(Tenant tenant, User employee);
    RecurringBooking save(RecurringBooking rb);
}
