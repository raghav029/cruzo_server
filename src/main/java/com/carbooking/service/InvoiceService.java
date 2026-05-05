package com.carbooking.service;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.enums.InvoiceStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.request.invoice.GenerateInvoiceRequest;
import com.carbooking.dto.request.invoice.MarkPaidRequest;
import com.carbooking.dto.response.invoice.InvoiceLineItemResponse;
import com.carbooking.dto.response.invoice.InvoiceResponse;
import com.carbooking.entity.*;
import com.carbooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository lineItemRepository;
    private final CorporateClientRepository corporateClientRepository;
    private final BookingRepository bookingRepository;
    private final TenantRepository tenantRepository;
    private final PricingConfigRepository pricingConfigRepository;

    // ── Generate ──────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse generate(GenerateInvoiceRequest request) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        CorporateClient client = corporateClientRepository.findById(request.getCorporateClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found"));
        if (!client.getTenant().getId().equals(tenantId)) throw new UnauthorizedException("Access denied");

        if (request.getBillingPeriodEnd().isBefore(request.getBillingPeriodStart())) {
            throw new BusinessRuleException("Billing period end must be after start");
        }

        // Prevent overlapping invoices for the same client
        boolean overlap = invoiceRepository
                .existsByCorporateClientAndBillingPeriodStartLessThanEqualAndBillingPeriodEndGreaterThanEqual(
                        client, request.getBillingPeriodEnd(), request.getBillingPeriodStart());
        if (overlap) {
            throw new BusinessRuleException("An invoice already exists for this client covering that period");
        }

        Instant from = request.getBillingPeriodStart().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = request.getBillingPeriodEnd().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Booking> bookings = bookingRepository.findUninvoicedCompletedBookings(client, from, to);
        if (bookings.isEmpty()) {
            throw new BusinessRuleException("No uninvoiced completed bookings found for this period");
        }

        // Snapshot tax rates from pricing config at generation time
        PricingConfig pricing = pricingConfigRepository.findByTenant(tenant).orElse(null);
        BigDecimal cgstPct = pricing != null ? pricing.getCgstPct() : new BigDecimal("9.00");
        BigDecimal sgstPct = pricing != null ? pricing.getSgstPct() : new BigDecimal("9.00");

        Invoice invoice = Invoice.builder()
                .tenant(tenant)
                .corporateClient(client)
                .invoiceNumber(generateInvoiceNumber(tenant))
                .billingPeriodStart(request.getBillingPeriodStart())
                .billingPeriodEnd(request.getBillingPeriodEnd())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .status(InvoiceStatus.DRAFT)
                .build();
        invoice = invoiceRepository.save(invoice);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalCancellationFees = BigDecimal.ZERO;

        for (Booking booking : bookings) {
            BigDecimal fare = booking.getFinalFare() != null
                    ? booking.getFinalFare()
                    : (booking.getEstimatedFare() != null ? booking.getEstimatedFare() : BigDecimal.ZERO);

            BigDecimal cgst = fare.multiply(cgstPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal sgst = fare.multiply(sgstPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = fare.add(cgst).add(sgst);

            LocalDate tripDate = booking.getTripCompletedAt() != null
                    ? booking.getTripCompletedAt().atZone(ZoneOffset.UTC).toLocalDate()
                    : booking.getScheduledAt().atZone(ZoneOffset.UTC).toLocalDate();

            InvoiceLineItem item = InvoiceLineItem.builder()
                    .tenant(tenant)
                    .invoice(invoice)
                    .booking(booking)
                    .description(booking.getPickupAddress() + " → " + booking.getDropAddress())
                    .tripDate(tripDate)
                    .vehicleType(booking.getVehicleTypeRequested())
                    .baseFare(fare)
                    .cgstAmount(cgst)
                    .sgstAmount(sgst)
                    .lineTotal(lineTotal)
                    .build();
            lineItemRepository.save(item);

            subtotal = subtotal.add(fare);
            totalCgst = totalCgst.add(cgst);
            totalSgst = totalSgst.add(sgst);

            if (booking.getCancellationFee() != null) {
                totalCancellationFees = totalCancellationFees.add(booking.getCancellationFee());
            }
        }

        invoice.setSubtotal(subtotal);
        invoice.setCgstAmount(totalCgst);
        invoice.setSgstAmount(totalSgst);
        invoice.setCancellationFees(totalCancellationFees);
        invoice.setTotalAmount(subtotal.add(totalCgst).add(totalSgst).add(totalCancellationFees));
        invoiceRepository.save(invoice);

        return toResponse(invoice, lineItemRepository.findByInvoice(invoice));
    }

    // ── List / Get ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> list(UUID corporateClientId, Pageable pageable) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        Page<Invoice> page;
        if (corporateClientId != null) {
            CorporateClient client = corporateClientRepository.findById(corporateClientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found"));
            if (!client.getTenant().getId().equals(tenantId)) throw new UnauthorizedException("Access denied");
            page = invoiceRepository.findByCorporateClient(client, pageable);
        } else {
            page = invoiceRepository.findByTenant(tenant, pageable);
        }
        return page.map(inv -> toResponse(inv, lineItemRepository.findByInvoice(inv)));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(UUID invoiceId) {
        Invoice invoice = findAndVerify(invoiceId);
        return toResponse(invoice, lineItemRepository.findByInvoice(invoice));
    }

    // ── Status transitions ────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse markSent(UUID invoiceId) {
        Invoice invoice = findAndVerify(invoiceId);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT invoices can be marked as SENT");
        }
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setSentAt(Instant.now());
        return toResponse(invoiceRepository.save(invoice), lineItemRepository.findByInvoice(invoice));
    }

    @Transactional
    public InvoiceResponse markPaid(UUID invoiceId, MarkPaidRequest request) {
        Invoice invoice = findAndVerify(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessRuleException("Invoice is already PAID");
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());
        if (request.getPaymentMode() != null) invoice.setPaymentMode(request.getPaymentMode());
        if (request.getPaymentReference() != null) invoice.setPaymentReference(request.getPaymentReference());
        return toResponse(invoiceRepository.save(invoice), lineItemRepository.findByInvoice(invoice));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private Invoice findAndVerify(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        if (!invoice.getTenant().getId().equals(SecurityUtils.getCurrentTenantId())) {
            throw new UnauthorizedException("Access denied");
        }
        return invoice;
    }

    private String generateInvoiceNumber(Tenant tenant) {
        long count = invoiceRepository.findByTenant(tenant, Pageable.unpaged()).getTotalElements() + 1;
        return "INV-" + tenant.getSubdomain().toUpperCase() + "-" + String.format("%04d", count);
    }

    private InvoiceResponse toResponse(Invoice inv, List<InvoiceLineItem> items) {
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

    private InvoiceLineItemResponse toLineItemResponse(InvoiceLineItem item) {
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
