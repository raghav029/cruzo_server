package com.carbooking.modules.b2c.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.request.b2c.CancelB2CBookingRequest;
import com.carbooking.dto.request.b2c.CreateB2CBookingRequest;
import com.carbooking.dto.response.b2c.B2CBookingResponse;
import com.carbooking.service.B2CBookingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "B2C — Bookings")
@RestController
@RequestMapping("/api/b2c/bookings")
@RequiredArgsConstructor
public class B2CBookingController {

    private final B2CBookingService b2cBookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<B2CBookingResponse>> create(
            @Valid @RequestBody CreateB2CBookingRequest req) {
        return ResponseHelper.created(
            b2cBookingService.create(SecurityUtils.getCurrentCustomerId(), req));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<B2CBookingResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseHelper.ok(new PagedResponse<>(
            b2cBookingService.list(SecurityUtils.getCurrentCustomerId(),
                PageRequest.of(page, size))));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<B2CBookingResponse>> get(
            @PathVariable UUID bookingId) {
        return ResponseHelper.ok(
            b2cBookingService.get(bookingId, SecurityUtils.getCurrentCustomerId()));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<B2CBookingResponse>> cancel(
            @PathVariable UUID bookingId,
            @RequestBody CancelB2CBookingRequest req) {
        return ResponseHelper.ok(
            b2cBookingService.cancel(bookingId, SecurityUtils.getCurrentCustomerId(), req));
    }
}
