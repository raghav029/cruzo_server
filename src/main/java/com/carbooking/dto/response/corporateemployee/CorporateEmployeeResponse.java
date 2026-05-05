package com.carbooking.dto.response.corporateemployee;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CorporateEmployeeResponse {
    private UUID id;
    private UUID tenantId;
    private UUID corporateClientId;
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private String employeeCode;
    private String department;
    private String designation;
    private Integer monthlyRideLimit;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
