package com.carbooking.modules.sos.controller;

import com.carbooking.common.enums.SosStatus;
import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.sos.application.SosAlertService;
import com.carbooking.modules.sos.dto.request.ResolveSosRequest;
import com.carbooking.modules.sos.dto.request.TriggerSosRequest;
import com.carbooking.modules.sos.dto.response.SosAlertResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "SOS Alerts")
@RestController
@RequestMapping("/api/sos")
@RequiredArgsConstructor
public class SosAlertController {

    private final SosAlertService sosAlertService;

    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'CORPORATE_ADMIN', 'EMPLOYEE', 'DRIVER')")
    @PostMapping
    public ResponseEntity<ApiResponse<SosAlertResponse>> trigger(@RequestBody TriggerSosRequest request) {
        return ResponseHelper.created(sosAlertService.trigger(request));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SosAlertResponse>>> list(
            @RequestParam(required = false) SosStatus status,
            Pageable pageable) {
        return ResponseHelper.ok(new PagedResponse<>(sosAlertService.list(status, pageable)));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SosAlertResponse>> get(@PathVariable UUID id) {
        return ResponseHelper.ok(sosAlertService.get(id));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<SosAlertResponse>> resolve(
            @PathVariable UUID id,
            @RequestBody ResolveSosRequest request) {
        return ResponseHelper.ok(sosAlertService.resolve(id, request));
    }
}
