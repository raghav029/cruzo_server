package com.carbooking.modules.tenant.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.tenant.application.TenantService;
import com.carbooking.modules.tenant.dto.request.CreateTenantRequest;
import com.carbooking.modules.tenant.dto.request.UpdateTenantRequest;
import com.carbooking.modules.tenant.dto.response.TenantResponse;
import com.carbooking.modules.tenant.dto.response.TenantStatsResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Tenants (Super Admin)")
@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponse>> create(
            @Valid @RequestBody CreateTenantRequest request) {
        return ResponseHelper.created(tenantService.create(request));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TenantResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseHelper.ok(new PagedResponse<>(tenantService.list(PageRequest.of(page, size))));
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> get(@PathVariable UUID tenantId) {
        return ResponseHelper.ok(tenantService.get(tenantId));
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> update(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateTenantRequest request) {
        return ResponseHelper.ok(tenantService.update(tenantId, request));
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> delete(@PathVariable UUID tenantId) {
        tenantService.delete(tenantId);
        return ResponseHelper.noContent();
    }

    @GetMapping("/{tenantId}/stats")
    public ResponseEntity<ApiResponse<TenantStatsResponse>> stats(@PathVariable UUID tenantId) {
        return ResponseHelper.ok(tenantService.getStats(tenantId));
    }
}
