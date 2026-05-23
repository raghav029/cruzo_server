package com.carbooking.dto.response.public_;

import com.carbooking.common.enums.VehicleType;
import com.carbooking.dto.response.vehicle.VehiclePackageResponse;
import com.carbooking.entity.enums.VehicleCategory;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter @Builder
public class PublicVehicleDetailResponse {
    private UUID id;
    private String make;
    private String model;
    private String color;
    private VehicleType vehicleType;
    private VehicleCategory category;
    private Short bagCapacity;
    private String description;
    private Set<String> amenities;
    private List<String> imageUrls;
    private List<VehiclePackageResponse> packages;
}
