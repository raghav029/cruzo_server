package com.carbooking.dto.response.public_;

import com.carbooking.common.enums.VehicleType;
import com.carbooking.entity.enums.VehicleCategory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Builder
public class PublicVehicleResponse {
    private UUID id;
    private String make;
    private String model;
    private String color;
    private VehicleType vehicleType;
    private VehicleCategory category;
    private Short bagCapacity;
    private String thumbnailUrl;
    private BigDecimal startingFromPrice;
}
