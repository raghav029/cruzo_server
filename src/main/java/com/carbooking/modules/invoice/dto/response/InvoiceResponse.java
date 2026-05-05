package com.carbooking.modules.invoice.dto.response;

import com.carbooking.common.enums.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class InvoiceResponse {
    private UUID id;
    private UUID tenantId;
    private UUID corporateClientId;
    private String corporateClientName;
    private String invoiceNumber;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private BigDecimal subtotal;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal cancellationFees;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
    private LocalDate dueDate;
    private Instant sentAt;
    private Instant paidAt;
    private String paymentMode;
    private String paymentReference;
    private String notes;
    private List<InvoiceLineItemResponse> lineItems;
    private Instant createdAt;
    private Instant updatedAt;
}
