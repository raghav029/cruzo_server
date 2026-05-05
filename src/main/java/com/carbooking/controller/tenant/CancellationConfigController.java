package com.carbooking.controller.tenant;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.dto.request.config.UpdateCancellationConfigRequest;
import com.carbooking.dto.response.config.CancellationConfigResponse;
import com.carbooking.service.CancellationConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Cancellation Config")
@RestController
@RequestMapping("/api/cancellation-config")
@RequiredArgsConstructor
public class CancellationConfigController {

    private final CancellationConfigService cancellationConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<CancellationConfigResponse>> get() {
        return ResponseEntity.ok(ApiResponse.ok(cancellationConfigService.get()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<CancellationConfigResponse>> update(
            @Valid @RequestBody UpdateCancellationConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(cancellationConfigService.update(request)));
    }
}
