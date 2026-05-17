package com.carbooking.modules.client.dto.request;

import com.carbooking.common.enums.BillingCycle;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateCorporateClientRequest {

    private String companyName;

    private String gstNumber;

    private String billingAddress;

    @Email(message = "Invalid billing email")
    private String billingEmail;

    private BillingCycle billingCycle;

    @DecimalMin(value = "0.0", message = "Credit limit cannot be negative")
    private BigDecimal creditLimit;

    private Boolean active;

    private BigDecimal maxBookingValue;
    private String allowedVehicleTypes;
}
