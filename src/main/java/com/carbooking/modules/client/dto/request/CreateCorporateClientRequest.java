package com.carbooking.modules.client.dto.request;

import com.carbooking.common.enums.BillingCycle;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateCorporateClientRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    private String gstNumber;

    private String billingAddress;

    @NotBlank(message = "Billing email is required")
    @Email(message = "Invalid billing email")
    private String billingEmail;

    @NotNull(message = "Billing cycle is required")
    private BillingCycle billingCycle;

    @DecimalMin(value = "0.0", message = "Credit limit cannot be negative")
    private BigDecimal creditLimit = BigDecimal.ZERO;
}
