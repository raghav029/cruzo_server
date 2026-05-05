package com.carbooking.modules.fleet.domain.port;

import com.carbooking.common.enums.DriverAvailability;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverPort {
    Optional<Driver> findById(UUID id);
    Page<Driver> findByTenant(Tenant tenant, Pageable pageable);
    Page<Driver> findByTenantAndAvailability(Tenant tenant, DriverAvailability availability, Pageable pageable);
    Optional<Driver> findByUser(User user);
    boolean existsByTenantAndLicenseNumber(Tenant tenant, String licenseNumber);
    List<Driver> findAvailableDriversByTenantOrderByLastUpdated(Tenant tenant);
    List<Driver> findByTenantAndAvailability(Tenant tenant, DriverAvailability availability);
    List<Driver> findByTenantAndLicenseExpiryBefore(Tenant tenant, LocalDate date);
    List<Driver> findByTenantAndInsuranceExpiryBefore(Tenant tenant, LocalDate date);
    long countByTenant(Tenant tenant);
    long countByTenantAndAvailability(Tenant tenant, DriverAvailability availability);
    Driver save(Driver driver);
}
