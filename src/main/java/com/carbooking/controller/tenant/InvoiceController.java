package com.carbooking.controller.tenant;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.dto.request.invoice.GenerateInvoiceRequest;
import com.carbooking.dto.request.invoice.MarkPaidRequest;
import com.carbooking.dto.response.invoice.InvoiceResponse;
import com.carbooking.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Invoices")
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<InvoiceResponse>> generate(
            @Valid @RequestBody GenerateInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(invoiceService.generate(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> list(
            @RequestParam(required = false) UUID corporateClientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                new PagedResponse<>(invoiceService.list(corporateClientId, PageRequest.of(page, size)))));
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> get(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.get(invoiceId)));
    }

    @PostMapping("/{invoiceId}/mark-sent")
    public ResponseEntity<ApiResponse<InvoiceResponse>> markSent(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.markSent(invoiceId)));
    }

    @PostMapping("/{invoiceId}/mark-paid")
    public ResponseEntity<ApiResponse<InvoiceResponse>> markPaid(
            @PathVariable UUID invoiceId,
            @RequestBody(required = false) MarkPaidRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                invoiceService.markPaid(invoiceId, request != null ? request : new MarkPaidRequest())));
    }
}
