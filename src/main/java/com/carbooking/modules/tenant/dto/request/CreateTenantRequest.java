package com.carbooking.modules.tenant.dto.request;

import com.carbooking.entity.enums.BookingMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTenantRequest {

    @NotBlank(message = "Company name is required")
    private String name;

    @NotBlank(message = "Subdomain is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Subdomain must be lowercase letters, numbers and hyphens only")
    @Size(min = 3, max = 50, message = "Subdomain must be between 3 and 50 characters")
    private String subdomain;

    @Email(message = "Invalid support email")
    private String supportEmail;

    @Size(max = 20)
    private String supportPhone;

    private BookingMode bookingMode = BookingMode.CORPORATE;

    private String logoUrl;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Primary color must be a valid hex color e.g. #3B82F6")
    private String primaryColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Secondary color must be a valid hex color e.g. #6B7280")
    private String secondaryColor;

    // Fleet Manager user to auto-create
    @NotBlank(message = "Fleet manager full name is required")
    private String fleetManagerName;

    @NotBlank(message = "Fleet manager email is required")
    @Email(message = "Invalid fleet manager email")
    private String fleetManagerEmail;

    @NotBlank(message = "Fleet manager phone is required")
    private String fleetManagerPhone;
}
