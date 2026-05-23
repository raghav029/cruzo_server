package com.carbooking.dto.response.b2c;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter @Builder
public class CustomerResponse {
    private UUID id;
    private String name;
    private String phone;
    private String email;
    private boolean verified;
    private Instant createdAt;
}
