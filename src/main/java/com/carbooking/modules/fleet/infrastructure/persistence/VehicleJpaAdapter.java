package com.carbooking.modules.fleet.infrastructure.persistence;

import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.common.enums.VehicleType;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.Vehicle;
import com.carbooking.modules.fleet.domain.port.VehiclePort;
import com.carbooking.repository.VehicleRepository;
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
public class VehicleJpaAdapter implements VehiclePort {

    private final VehicleRepository repo;

    @Override public Optional<Vehicle> findById(UUID id) { return repo.findById(id); }
    @Override public Page<Vehicle> findByTenant(Tenant tenant, Pageable pageable) { return repo.findByTenant(tenant, pageable); }
    @Override public Page<Vehicle> findByTenantAndStatus(Tenant tenant, VehicleStatus status, Pageable pageable) { return repo.findByTenantAndStatus(tenant, status, pageable); }
    @Override public boolean existsByTenantAndPlateNumber(Tenant tenant, String plateNumber) { return repo.existsByTenantAndPlateNumber(tenant, plateNumber); }
    @Override public List<Vehicle> findByTenantAndStatusAndVehicleType(Tenant tenant, VehicleStatus status, VehicleType type) { return repo.findByTenantAndStatusAndVehicleType(tenant, status, type); }
    @Override public List<Vehicle> findByTenantAndInsuranceExpiryBeforeAndStatusNot(Tenant tenant, LocalDate date, VehicleStatus status) { return repo.findByTenantAndInsuranceExpiryBeforeAndStatusNot(tenant, date, status); }
    @Override public List<Vehicle> findByTenantAndFitnessExpiryBeforeAndStatusNot(Tenant tenant, LocalDate date, VehicleStatus status) { return repo.findByTenantAndFitnessExpiryBeforeAndStatusNot(tenant, date, status); }
    @Override public long countByTenant(Tenant tenant) { return repo.countByTenant(tenant); }
    @Override public long countByTenantAndStatus(Tenant tenant, VehicleStatus status) { return repo.countByTenantAndStatus(tenant, status); }
    @Override public Vehicle save(Vehicle vehicle) { return repo.save(vehicle); }
}
