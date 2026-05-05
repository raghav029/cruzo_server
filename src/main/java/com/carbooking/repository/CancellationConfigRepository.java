package com.carbooking.repository;

import com.carbooking.entity.CancellationConfig;
import com.carbooking.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CancellationConfigRepository extends JpaRepository<CancellationConfig, UUID> {
    Optional<CancellationConfig> findByTenant(Tenant tenant);
}
