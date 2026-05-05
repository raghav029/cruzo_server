package com.carbooking.modules.invoice.application;

import com.carbooking.common.enums.InvoiceStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.*;
import com.carbooking.modules.invoice.dto.request.GenerateInvoiceRequest;
import com.carbooking.modules.invoice.dto.request.MarkPaidRequest;
import com.carbooking.modules.invoice.dto.response.InvoiceLineItemResponse;
import com.carbooking.modules.invoice.dto.response.InvoiceResponse;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.client.domain.port.CorporateClientPort;
import com.carbooking.modules.config.domain.port.PricingConfigPort;
import com.carbooking.modules.invoice.domain.port.InvoiceLineItemPort;
import com.carbooking.modules.invoice.domain.port.InvoicePort;
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
import java.util.stream.Collectors;
import com.carbooking.modules.invoice.application.InvoiceMapper;

@Service
public class InvoiceService extends TenantSupport {

    private final InvoiceMapper invoiceMapper;
    private final InvoicePort invoiceRepository;
    private final InvoiceLineItemPort lineItemRepository;
    private final CorporateClientPort corporateClientRepository;
    private final BookingPort bookingRepository;
    private final PricingConfigPort pricingConfigRepository;

    public InvoiceService(InvoicePort invoiceRepository, InvoiceLineItemPort lineItemRepository,
                          CorporateClientPort corporateClientRepository,
                          BookingPort bookingRepository,
                          PricingConfigPort pricingConfigRepository,
                                 InvoiceMapper invoiceMapper) {
        this.invoiceRepository = invoiceRepository;
        this.lineItemRepository = lineItemRepository;
        this.corporateClientRepository = corporateClientRepository;
        this.bookingRepository = bookingRepository;
        this.pricingConfigRepository = pricingConfigRepository;
        this.invoiceMapper = invoiceMapper;
    }

    @Transactional
    public InvoiceResponse generate(GenerateInvoiceRequest request) {
        Tenant tenant = requireTenant();

        CorporateClient client = corporateClientRepository.findById(request.getCorporateClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found"));
        assertSameTenant(client.getTenant().getId());

        if (request.getBillingPeriodEnd().isBefore(request.getBillingPeriodStart())) {
            throw new BusinessRuleException("Billing period end must be after start");
        }

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

        return invoiceMapper.toResponse(invoice, lineItemRepository.findByInvoice(invoice));
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> list(UUID corporateClientId, Pageable pageable) {
        Tenant tenant = requireTenant();

        Page<Invoice> page;
        if (corporateClientId != null) {
            CorporateClient client = corporateClientRepository.findById(corporateClientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found"));
            assertSameTenant(client.getTenant().getId());
            page = invoiceRepository.findByCorporateClient(client, pageable);
        } else {
            page = invoiceRepository.findByTenant(tenant, pageable);
        }
        return page.map(inv -> invoiceMapper.toResponse(inv, lineItemRepository.findByInvoice(inv)));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(UUID invoiceId) {
        Invoice invoice = findInvoice(invoiceId);
        return invoiceMapper.toResponse(invoice, lineItemRepository.findByInvoice(invoice));
    }

    @Transactional
    public InvoiceResponse markSent(UUID invoiceId) {
        Invoice invoice = findInvoice(invoiceId);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT invoices can be marked as SENT");
        }
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setSentAt(Instant.now());
        return invoiceMapper.toResponse(invoiceRepository.save(invoice), lineItemRepository.findByInvoice(invoice));
    }

    @Transactional
    public InvoiceResponse markPaid(UUID invoiceId, MarkPaidRequest request) {
        Invoice invoice = findInvoice(invoiceId);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessRuleException("Invoice is already PAID");
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());
        if (request.getPaymentMode() != null) invoice.setPaymentMode(request.getPaymentMode());
        if (request.getPaymentReference() != null) invoice.setPaymentReference(request.getPaymentReference());
        return invoiceMapper.toResponse(invoiceRepository.save(invoice), lineItemRepository.findByInvoice(invoice));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Invoice findInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        assertSameTenant(invoice.getTenant().getId());
        return invoice;
    }

    private String generateInvoiceNumber(Tenant tenant) {
        long count = invoiceRepository.findByTenant(tenant, Pageable.unpaged()).getTotalElements() + 1;
        return "INV-" + tenant.getSubdomain().toUpperCase() + "-" + String.format("%04d", count);
    }


}
