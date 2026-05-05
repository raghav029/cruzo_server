package com.carbooking.modules.tenant.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTenantRequest {

    private String name;

    @Email(message = "Invalid support email")
    private String supportEmail;

    @Size(max = 20)
    private String supportPhone;

    private String logoUrl;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Primary color must be a valid hex color e.g. #3B82F6")
    private String primaryColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Secondary color must be a valid hex color e.g. #6B7280")
    private String secondaryColor;

    // null = no change, true = activate, false = suspend
    private Boolean active;
}
