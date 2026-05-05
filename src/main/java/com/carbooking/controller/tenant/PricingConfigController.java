package com.carbooking.controller.tenant;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.dto.request.config.UpdatePricingConfigRequest;
import com.carbooking.dto.response.config.PricingConfigResponse;
import com.carbooking.service.PricingConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Pricing Config")
@RestController
@RequestMapping("/api/pricing-config")
@RequiredArgsConstructor
public class PricingConfigController {

    private final PricingConfigService pricingConfigService;

    @GetMapping
    public ResponseEntity<ApiResponse<PricingConfigResponse>> get() {
        return ResponseEntity.ok(ApiResponse.ok(pricingConfigService.get()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PricingConfigResponse>> update(
            @Valid @RequestBody UpdatePricingConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(pricingConfigService.update(request)));
    }
}
