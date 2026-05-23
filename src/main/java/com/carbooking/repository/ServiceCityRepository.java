package com.carbooking.repository;

import com.carbooking.entity.ServiceCity;
import com.carbooking.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceCityRepository extends JpaRepository<ServiceCity, UUID> {
    List<ServiceCity> findByTenant(Tenant tenant);
    List<ServiceCity> findByTenantIdAndActiveTrue(UUID tenantId);
    boolean existsByTenantAndNameIgnoreCase(Tenant tenant, String name);
    Optional<ServiceCity> findByIdAndTenant(UUID id, Tenant tenant);
}
