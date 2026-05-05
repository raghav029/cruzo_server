package com.carbooking.modules.invoice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class GenerateInvoiceRequest {

    @NotNull(message = "Corporate client is required")
    private UUID corporateClientId;

    @NotNull(message = "Billing period start is required")
    private LocalDate billingPeriodStart;

    @NotNull(message = "Billing period end is required")
    private LocalDate billingPeriodEnd;

    private LocalDate dueDate;

    private String notes;
}
