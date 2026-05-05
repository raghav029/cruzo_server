package com.carbooking.modules.client.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.client.application.CorporateClientService;
import com.carbooking.modules.client.dto.request.CreateCorporateAdminRequest;
import com.carbooking.modules.client.dto.request.CreateCorporateClientRequest;
import com.carbooking.modules.client.dto.request.UpdateCorporateClientRequest;
import com.carbooking.modules.client.dto.response.CorporateAdminResponse;
import com.carbooking.modules.client.dto.response.CorporateClientResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "Corporate Clients")
@RestController
@RequestMapping("/api/corporate-clients")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FLEET_MANAGER')")
public class CorporateClientController {

    private final CorporateClientService clientService;

    @PostMapping
    public ResponseEntity<ApiResponse<CorporateClientResponse>> create(
            @Valid @RequestBody CreateCorporateClientRequest request) {
        return ResponseHelper.created(clientService.create(request));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CorporateClientResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseHelper.ok(new PagedResponse<>(clientService.list(PageRequest.of(page, size))));
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ApiResponse<CorporateClientResponse>> get(@PathVariable UUID clientId) {
        return ResponseHelper.ok(clientService.get(clientId));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<ApiResponse<CorporateClientResponse>> update(
            @PathVariable UUID clientId,
            @Valid @RequestBody UpdateCorporateClientRequest request) {
        return ResponseHelper.ok(clientService.update(clientId, request));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> delete(@PathVariable UUID clientId) {
        clientService.delete(clientId);
        return ResponseHelper.noContent();
    }

    @PostMapping("/{clientId}/admins")
    public ResponseEntity<ApiResponse<CorporateAdminResponse>> createAdmin(
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateCorporateAdminRequest request) {
        return ResponseHelper.created(clientService.createAdmin(clientId, request));
    }
}
