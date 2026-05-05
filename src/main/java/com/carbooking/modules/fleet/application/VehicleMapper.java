package com.carbooking.modules.fleet.application;

import com.carbooking.entity.Vehicle;
import com.carbooking.modules.fleet.dto.response.VehicleResponse;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

    public VehicleResponse toResponse(Vehicle v) {
        return VehicleResponse.builder()
                .id(v.getId())
                .tenantId(v.getTenant().getId())
                .plateNumber(v.getPlateNumber())
                .vehicleType(v.getVehicleType())
                .make(v.getMake())
                .model(v.getModel())
                .year(v.getYear())
                .color(v.getColor())
                .status(v.getStatus())
                .insuranceExpiry(v.getInsuranceExpiry())
                .fitnessExpiry(v.getFitnessExpiry())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}
