package com.carbooking.controller.tenant;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.dto.request.corporateclient.CreateCorporateAdminRequest;
import com.carbooking.dto.request.corporateclient.CreateCorporateClientRequest;
import com.carbooking.dto.request.corporateclient.UpdateCorporateClientRequest;
import com.carbooking.dto.response.corporateclient.CorporateAdminResponse;
import com.carbooking.dto.response.corporateclient.CorporateClientResponse;
import com.carbooking.service.CorporateClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Corporate Clients")
@RestController
@RequestMapping("/api/corporate-clients")
@RequiredArgsConstructor
public class CorporateClientController {

    private final CorporateClientService clientService;

    @PostMapping
    public ResponseEntity<ApiResponse<CorporateClientResponse>> create(
            @Valid @RequestBody CreateCorporateClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(clientService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CorporateClientResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                new PagedResponse<>(clientService.list(PageRequest.of(page, size)))));
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ApiResponse<CorporateClientResponse>> get(@PathVariable UUID clientId) {
        return ResponseEntity.ok(ApiResponse.ok(clientService.get(clientId)));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<ApiResponse<CorporateClientResponse>> update(
            @PathVariable UUID clientId,
            @Valid @RequestBody UpdateCorporateClientRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(clientService.update(clientId, request)));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> delete(@PathVariable UUID clientId) {
        clientService.delete(clientId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{clientId}/admins")
    public ResponseEntity<ApiResponse<CorporateAdminResponse>> createAdmin(
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateCorporateAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(clientService.createAdmin(clientId, request)));
    }
}
