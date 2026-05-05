package com.carbooking.modules.invoice.application;

import com.carbooking.entity.Invoice;
import com.carbooking.entity.InvoiceLineItem;
import com.carbooking.modules.invoice.dto.response.InvoiceLineItemResponse;
import com.carbooking.modules.invoice.dto.response.InvoiceResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class InvoiceMapper {

    public InvoiceResponse toResponse(Invoice inv, List<InvoiceLineItem> items) {
        return InvoiceResponse.builder()
                .id(inv.getId())
                .tenantId(inv.getTenant().getId())
                .corporateClientId(inv.getCorporateClient().getId())
                .corporateClientName(inv.getCorporateClient().getCompanyName())
                .invoiceNumber(inv.getInvoiceNumber())
                .billingPeriodStart(inv.getBillingPeriodStart())
                .billingPeriodEnd(inv.getBillingPeriodEnd())
                .subtotal(inv.getSubtotal())
                .cgstAmount(inv.getCgstAmount())
                .sgstAmount(inv.getSgstAmount())
                .cancellationFees(inv.getCancellationFees())
                .totalAmount(inv.getTotalAmount())
                .status(inv.getStatus())
                .dueDate(inv.getDueDate())
                .sentAt(inv.getSentAt())
                .paidAt(inv.getPaidAt())
                .paymentMode(inv.getPaymentMode())
                .paymentReference(inv.getPaymentReference())
                .notes(inv.getNotes())
                .lineItems(items.stream().map(this::toLineItemResponse).collect(Collectors.toList()))
                .createdAt(inv.getCreatedAt())
                .updatedAt(inv.getUpdatedAt())
                .build();
    }

    public InvoiceLineItemResponse toLineItemResponse(InvoiceLineItem item) {
        return InvoiceLineItemResponse.builder()
                .id(item.getId())
                .bookingId(item.getBooking().getId())
                .description(item.getDescription())
                .tripDate(item.getTripDate())
                .vehicleType(item.getVehicleType())
                .baseFare(item.getBaseFare())
                .cgstAmount(item.getCgstAmount())
                .sgstAmount(item.getSgstAmount())
                .lineTotal(item.getLineTotal())
                .build();
    }
}
