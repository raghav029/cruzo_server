package com.carbooking.modules.addon.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.modules.addon.application.AddonService;
import com.carbooking.modules.addon.dto.request.AddBookingAddonsRequest;
import com.carbooking.modules.addon.dto.request.CreateAddonRequest;
import com.carbooking.modules.addon.dto.request.UpdateAddonRequest;
import com.carbooking.modules.addon.dto.response.AddonResponse;
import com.carbooking.modules.addon.dto.response.BookingAddonResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Addons")
@RestController
@RequiredArgsConstructor
public class AddonController {

    private final AddonService addonService;

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @PostMapping("/api/addons")
    public ResponseEntity<ApiResponse<AddonResponse>> create(@Valid @RequestBody CreateAddonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(addonService.create(request)));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @GetMapping("/api/addons")
    public ResponseEntity<ApiResponse<PagedResponse<AddonResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(new PagedResponse<>(addonService.list(PageRequest.of(page, size)))));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @PutMapping("/api/addons/{id}")
    public ResponseEntity<ApiResponse<AddonResponse>> update(
            @PathVariable UUID id,
            @RequestBody UpdateAddonRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(addonService.update(id, request)));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @DeleteMapping("/api/addons/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        addonService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/bookings/{bookingId}/addons")
    public ResponseEntity<ApiResponse<List<BookingAddonResponse>>> addToBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody AddBookingAddonsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(addonService.addToBooking(bookingId, request)));
    }

    @GetMapping("/api/bookings/{bookingId}/addons")
    public ResponseEntity<ApiResponse<List<BookingAddonResponse>>> getBookingAddons(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.ok(addonService.getBookingAddons(bookingId)));
    }
}
