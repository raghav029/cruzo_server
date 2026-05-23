package com.carbooking.repository;

import com.carbooking.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByPhoneAndTenantId(String phone, UUID tenantId);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndCreatedAtBetween(UUID tenantId, Instant from, Instant to);
}
