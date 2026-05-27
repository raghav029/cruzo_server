package com.carbooking.modules.promo.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.modules.promo.application.PromoCodeService;
import com.carbooking.modules.promo.dto.request.CreatePromoCodeRequest;
import com.carbooking.modules.promo.dto.response.PromoCodeResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Promo Codes")
@RestController
@RequestMapping("/api/promo-codes")
@RequiredArgsConstructor
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @PostMapping
    public ResponseEntity<ApiResponse<PromoCodeResponse>> create(
            @Valid @RequestBody CreatePromoCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(promoCodeService.create(request)));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PromoCodeResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = promoCodeService.list(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(new PagedResponse<>(result)));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromoCodeResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(promoCodeService.get(id)));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        promoCodeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
