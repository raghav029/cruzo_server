package com.carbooking.modules.client.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateCorporateEmployeeRequest {
    private String phone;
    private String employeeCode;
    private String department;
    private String designation;
    private Integer monthlyRideLimit;
    private Boolean active;

    private BigDecimal maxBookingValueOverride;
    private String allowedVehicleTypesOverride;
}
