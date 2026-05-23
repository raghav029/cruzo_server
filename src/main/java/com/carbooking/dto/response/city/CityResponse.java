package com.carbooking.dto.response.city;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter @Builder
public class CityResponse {
    private UUID id;
    private String name;
    private boolean active;
    private Instant createdAt;
}
