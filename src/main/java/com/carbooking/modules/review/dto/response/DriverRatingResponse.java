package com.carbooking.modules.review.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter @Builder
public class DriverRatingResponse {

    private UUID driverId;
    private String driverName;
    private double averageRating;
    private long reviewCount;
}
