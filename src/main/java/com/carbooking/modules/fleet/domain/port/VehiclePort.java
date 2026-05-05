package com.carbooking.modules.fleet.domain.port;

import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.common.enums.VehicleType;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehiclePort {
    Optional<Vehicle> findById(UUID id);
    Page<Vehicle> findByTenant(Tenant tenant, Pageable pageable);
    Page<Vehicle> findByTenantAndStatus(Tenant tenant, VehicleStatus status, Pageable pageable);
    boolean existsByTenantAndPlateNumber(Tenant tenant, String plateNumber);
    List<Vehicle> findByTenantAndStatusAndVehicleType(Tenant tenant, VehicleStatus status, VehicleType type);
    List<Vehicle> findByTenantAndInsuranceExpiryBeforeAndStatusNot(Tenant tenant, LocalDate date, VehicleStatus status);
    List<Vehicle> findByTenantAndFitnessExpiryBeforeAndStatusNot(Tenant tenant, LocalDate date, VehicleStatus status);
    long countByTenant(Tenant tenant);
    long countByTenantAndStatus(Tenant tenant, VehicleStatus status);
    Vehicle save(Vehicle vehicle);
}
