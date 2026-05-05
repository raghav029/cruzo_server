package com.carbooking.modules.invoice.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.invoice.application.InvoiceService;
import com.carbooking.modules.invoice.dto.request.GenerateInvoiceRequest;
import com.carbooking.modules.invoice.dto.request.MarkPaidRequest;
import com.carbooking.modules.invoice.dto.response.InvoiceResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Invoices")
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<InvoiceResponse>> generate(
            @Valid @RequestBody GenerateInvoiceRequest request) {
        return ResponseHelper.created(invoiceService.generate(request));
    }

    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'CORPORATE_ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> list(
            @RequestParam(required = false) UUID corporateClientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseHelper.ok(new PagedResponse<>(invoiceService.list(corporateClientId, PageRequest.of(page, size))));
    }

    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'CORPORATE_ADMIN')")
    @GetMapping("/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> get(@PathVariable UUID invoiceId) {
        return ResponseHelper.ok(invoiceService.get(invoiceId));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @PostMapping("/{invoiceId}/mark-sent")
    public ResponseEntity<ApiResponse<InvoiceResponse>> markSent(@PathVariable UUID invoiceId) {
        return ResponseHelper.ok(invoiceService.markSent(invoiceId));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @PostMapping("/{invoiceId}/mark-paid")
    public ResponseEntity<ApiResponse<InvoiceResponse>> markPaid(
            @PathVariable UUID invoiceId,
            @RequestBody(required = false) MarkPaidRequest request) {
        return ResponseHelper.ok(invoiceService.markPaid(invoiceId, request != null ? request : new MarkPaidRequest()));
    }
}
