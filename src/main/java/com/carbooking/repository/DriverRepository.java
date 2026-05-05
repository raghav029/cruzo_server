package com.carbooking.repository;

import com.carbooking.common.enums.DriverAvailability;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {
    Page<Driver> findByTenant(Tenant tenant, Pageable pageable);
    Page<Driver> findByTenantAndAvailability(Tenant tenant, DriverAvailability availability, Pageable pageable);
    Optional<Driver> findByUser(User user);
    boolean existsByTenantAndLicenseNumber(Tenant tenant, String licenseNumber);
    List<Driver> findByTenantAndAvailability(Tenant tenant, DriverAvailability availability);

    @Query("SELECT d FROM Driver d WHERE d.tenant = :tenant AND d.availability = 'AVAILABLE' ORDER BY d.updatedAt ASC")
    List<Driver> findAvailableDriversByTenantOrderByLastUpdated(Tenant tenant);

    long countByTenant(Tenant tenant);
    long countByTenantAndAvailability(Tenant tenant, DriverAvailability availability);

    List<Driver> findByTenantAndLicenseExpiryBefore(Tenant tenant, LocalDate date);
    List<Driver> findByTenantAndInsuranceExpiryBefore(Tenant tenant, LocalDate date);
}
