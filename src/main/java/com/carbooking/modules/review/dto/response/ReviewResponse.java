package com.carbooking.modules.review.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter @Builder
public class ReviewResponse {

    private UUID id;
    private UUID bookingId;
    private UUID driverId;
    private String driverName;
    private int rating;
    private String comment;
    private List<String> tags;
    private String reviewerType;
    private Instant createdAt;
}
