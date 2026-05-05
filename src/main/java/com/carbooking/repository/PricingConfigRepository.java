package com.carbooking.repository;

import com.carbooking.entity.PricingConfig;
import com.carbooking.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PricingConfigRepository extends JpaRepository<PricingConfig, UUID> {
    Optional<PricingConfig> findByTenant(Tenant tenant);
}
