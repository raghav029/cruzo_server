package com.carbooking.modules.b2c.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.modules.promo.application.PromoCodeService;
import com.carbooking.modules.promo.dto.request.ValidatePromoRequest;
import com.carbooking.modules.promo.dto.response.ValidatePromoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "B2C - Promo Codes")
@RestController
@RequestMapping("/api/b2c/promo-codes")
@RequiredArgsConstructor
public class B2CPromoController {

    private final PromoCodeService promoCodeService;

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<ValidatePromoResponse>> validate(
            @Valid @RequestBody ValidatePromoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(promoCodeService.validate(request)));
    }
}
