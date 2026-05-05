package com.carbooking.controller.admin;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.dto.request.tenant.CreateTenantRequest;
import com.carbooking.dto.request.tenant.UpdateTenantRequest;
import com.carbooking.dto.response.tenant.TenantResponse;
import com.carbooking.dto.response.tenant.TenantStatsResponse;
import com.carbooking.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Tenants (Super Admin)")
@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponse>> create(
            @Valid @RequestBody CreateTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tenant created successfully", tenantService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TenantResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.ok(new PagedResponse<>(tenantService.list(PageRequest.of(page, size)))));
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> get(
            @PathVariable UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.get(tenantId)));
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> update(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.update(tenantId, request)));
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> delete(@PathVariable UUID tenantId) {
        tenantService.delete(tenantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tenantId}/stats")
    public ResponseEntity<ApiResponse<TenantStatsResponse>> stats(
            @PathVariable UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.getStats(tenantId)));
    }
}
