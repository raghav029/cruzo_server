package com.carbooking.modules.client.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateCorporateEmployeeRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    private String email;

    private String phone;

    private String employeeCode;

    private String department;

    private String designation;

    private Integer monthlyRideLimit;

    private BigDecimal maxBookingValueOverride;
    private String allowedVehicleTypesOverride;
}
