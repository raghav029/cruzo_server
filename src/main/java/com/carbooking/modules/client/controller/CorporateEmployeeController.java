package com.carbooking.modules.client.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.client.application.CorporateEmployeeService;
import com.carbooking.modules.client.dto.request.CreateCorporateEmployeeRequest;
import com.carbooking.modules.client.dto.request.UpdateCorporateEmployeeRequest;
import com.carbooking.modules.client.dto.response.CorporateEmployeeResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "Corporate Employees")
@RestController
@RequestMapping("/api/corporate-clients/{clientId}/employees")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FLEET_MANAGER', 'CORPORATE_ADMIN')")
public class CorporateEmployeeController {

    private final CorporateEmployeeService employeeService;

    @PostMapping
    public ResponseEntity<ApiResponse<CorporateEmployeeResponse>> create(
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateCorporateEmployeeRequest request) {
        return ResponseHelper.created(employeeService.create(clientId, request));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CorporateEmployeeResponse>>> list(
            @PathVariable UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseHelper.ok(new PagedResponse<>(employeeService.list(clientId, PageRequest.of(page, size))));
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<CorporateEmployeeResponse>> get(
            @PathVariable UUID clientId,
            @PathVariable UUID employeeId) {
        return ResponseHelper.ok(employeeService.get(clientId, employeeId));
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<CorporateEmployeeResponse>> update(
            @PathVariable UUID clientId,
            @PathVariable UUID employeeId,
            @Valid @RequestBody UpdateCorporateEmployeeRequest request) {
        return ResponseHelper.ok(employeeService.update(clientId, employeeId, request));
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID clientId,
            @PathVariable UUID employeeId) {
        employeeService.delete(clientId, employeeId);
        return ResponseHelper.noContent();
    }
}
