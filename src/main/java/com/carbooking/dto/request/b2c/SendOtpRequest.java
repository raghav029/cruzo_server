package com.carbooking.dto.request.b2c;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class SendOtpRequest {
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter valid 10-digit mobile number")
    private String phone;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "tenantId is required")
    private UUID tenantId;
}
