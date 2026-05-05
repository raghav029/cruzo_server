package com.carbooking.controller.tenant;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.dto.request.corporateemployee.CreateCorporateEmployeeRequest;
import com.carbooking.dto.request.corporateemployee.UpdateCorporateEmployeeRequest;
import com.carbooking.dto.response.corporateemployee.CorporateEmployeeResponse;
import com.carbooking.service.CorporateEmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Corporate Employees")
@RestController
@RequestMapping("/api/corporate-clients/{clientId}/employees")
@RequiredArgsConstructor
public class CorporateEmployeeController {

    private final CorporateEmployeeService employeeService;

    @PostMapping
    public ResponseEntity<ApiResponse<CorporateEmployeeResponse>> create(
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateCorporateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(employeeService.create(clientId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CorporateEmployeeResponse>>> list(
            @PathVariable UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                new PagedResponse<>(employeeService.list(clientId, PageRequest.of(page, size)))));
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<CorporateEmployeeResponse>> get(
            @PathVariable UUID clientId,
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.get(clientId, employeeId)));
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<CorporateEmployeeResponse>> update(
            @PathVariable UUID clientId,
            @PathVariable UUID employeeId,
            @Valid @RequestBody UpdateCorporateEmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.update(clientId, employeeId, request)));
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID clientId,
            @PathVariable UUID employeeId) {
        employeeService.delete(clientId, employeeId);
        return ResponseEntity.noContent().build();
    }
}
