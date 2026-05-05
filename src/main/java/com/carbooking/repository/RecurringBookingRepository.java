package com.carbooking.repository;

import com.carbooking.entity.RecurringBooking;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecurringBookingRepository extends JpaRepository<RecurringBooking, UUID> {
    List<RecurringBooking> findByTenantAndIsActiveTrue(Tenant tenant);
    List<RecurringBooking> findByTenantAndEmployeeAndIsActiveTrue(Tenant tenant, User employee);
    List<RecurringBooking> findByIsActiveTrue();
}
