package com.carbooking.repository;

import com.carbooking.entity.VehiclePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehiclePackageRepository extends JpaRepository<VehiclePackage, UUID> {
    List<VehiclePackage> findByVehicleIdAndActiveTrue(UUID vehicleId);
    Optional<VehiclePackage> findByIdAndVehicleId(UUID id, UUID vehicleId);
}
