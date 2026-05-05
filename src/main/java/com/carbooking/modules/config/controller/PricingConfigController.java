package com.carbooking.modules.config.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.config.application.PricingConfigService;
import com.carbooking.modules.config.dto.request.UpdatePricingConfigRequest;
import com.carbooking.modules.config.dto.response.PricingConfigResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "Pricing Config")
@RestController
@RequestMapping("/api/pricing-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FLEET_MANAGER')")
public class PricingConfigController {

    private final PricingConfigService pricingConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<PricingConfigResponse>> get() {
        return ResponseHelper.ok(pricingConfigService.get());
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PricingConfigResponse>> update(
            @Valid @RequestBody UpdatePricingConfigRequest request) {
        return ResponseHelper.ok(pricingConfigService.update(request));
    }
}
