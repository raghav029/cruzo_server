package com.carbooking.modules.fleet.infrastructure.persistence;

import com.carbooking.common.enums.DriverAvailability;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.modules.fleet.domain.port.DriverPort;
import com.carbooking.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DriverJpaAdapter implements DriverPort {

    private final DriverRepository repo;

    @Override public Optional<Driver> findById(UUID id) { return repo.findById(id); }
    @Override public Page<Driver> findByTenant(Tenant tenant, Pageable pageable) { return repo.findByTenant(tenant, pageable); }
    @Override public Page<Driver> findByTenantAndAvailability(Tenant tenant, DriverAvailability availability, Pageable pageable) { return repo.findByTenantAndAvailability(tenant, availability, pageable); }
    @Override public Optional<Driver> findByUser(User user) { return repo.findByUser(user); }
    @Override public boolean existsByTenantAndLicenseNumber(Tenant tenant, String licenseNumber) { return repo.existsByTenantAndLicenseNumber(tenant, licenseNumber); }
    @Override public List<Driver> findAvailableDriversByTenantOrderByLastUpdated(Tenant tenant) { return repo.findAvailableDriversByTenantOrderByLastUpdated(tenant); }
    @Override public List<Driver> findByTenantAndAvailability(Tenant tenant, DriverAvailability availability) { return repo.findByTenantAndAvailability(tenant, availability); }
    @Override public List<Driver> findByTenantAndLicenseExpiryBefore(Tenant tenant, LocalDate date) { return repo.findByTenantAndLicenseExpiryBefore(tenant, date); }
    @Override public List<Driver> findByTenantAndInsuranceExpiryBefore(Tenant tenant, LocalDate date) { return repo.findByTenantAndInsuranceExpiryBefore(tenant, date); }
    @Override public long countByTenant(Tenant tenant) { return repo.countByTenant(tenant); }
    @Override public long countByTenantAndAvailability(Tenant tenant, DriverAvailability availability) { return repo.countByTenantAndAvailability(tenant, availability); }
    @Override public Driver save(Driver driver) { return repo.save(driver); }
}
