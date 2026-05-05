package com.carbooking.dto.response.auth;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class LoginResponse {

    private String token;
    private UUID userId;
    private String role;
    private UUID tenantId;
    private String fullName;
    private String email;
    private Instant expiresAt;
}
