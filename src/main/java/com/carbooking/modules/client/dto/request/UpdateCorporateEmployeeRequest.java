package com.carbooking.modules.client.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCorporateEmployeeRequest {
    private String phone;
    private String employeeCode;
    private String department;
    private String designation;
    private Integer monthlyRideLimit;
    private Boolean active;
}
