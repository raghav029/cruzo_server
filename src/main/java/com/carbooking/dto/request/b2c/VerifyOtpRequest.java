package com.carbooking.dto.request.b2c;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class VerifyOtpRequest {
    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits")
    private String otp;

    @NotNull(message = "tenantId is required")
    private UUID tenantId;
}
