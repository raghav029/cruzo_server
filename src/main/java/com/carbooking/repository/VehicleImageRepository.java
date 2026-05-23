package com.carbooking.repository;

import com.carbooking.entity.VehicleImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleImageRepository extends JpaRepository<VehicleImage, UUID> {
    List<VehicleImage> findByVehicleIdOrderByDisplayOrderAsc(UUID vehicleId);
}
