package com.carbooking.modules.config.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.config.application.CancellationConfigService;
import com.carbooking.modules.config.dto.request.UpdateCancellationConfigRequest;
import com.carbooking.modules.config.dto.response.CancellationConfigResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "Cancellation Config")
@RestController
@RequestMapping("/api/cancellation-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FLEET_MANAGER')")
public class CancellationConfigController {

    private final CancellationConfigService cancellationConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<CancellationConfigResponse>> get() {
        return ResponseHelper.ok(cancellationConfigService.get());
    }

    @PutMapping
    public ResponseEntity<ApiResponse<CancellationConfigResponse>> update(
            @Valid @RequestBody UpdateCancellationConfigRequest request) {
        return ResponseHelper.ok(cancellationConfigService.update(request));
    }
}
