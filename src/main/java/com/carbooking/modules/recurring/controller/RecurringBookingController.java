package com.carbooking.modules.recurring.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.recurring.application.RecurringBookingService;
import com.carbooking.modules.recurring.dto.request.CreateRecurringBookingRequest;
import com.carbooking.modules.recurring.dto.response.RecurringBookingResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Recurring Bookings")
@RestController
@RequestMapping("/api/recurring-bookings")
@RequiredArgsConstructor
public class RecurringBookingController {

    private final RecurringBookingService recurringBookingService;

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CORPORATE_ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<RecurringBookingResponse>> create(
            @Valid @RequestBody CreateRecurringBookingRequest request) {
        return ResponseHelper.created(recurringBookingService.create(request));
    }

    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'EMPLOYEE', 'CORPORATE_ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecurringBookingResponse>>> list() {
        return ResponseHelper.ok(recurringBookingService.list());
    }

    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'EMPLOYEE', 'CORPORATE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringBookingResponse>> get(@PathVariable UUID id) {
        return ResponseHelper.ok(recurringBookingService.get(id));
    }

    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'EMPLOYEE', 'CORPORATE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringBookingResponse>> deactivate(@PathVariable UUID id) {
        return ResponseHelper.ok(recurringBookingService.deactivate(id));
    }
}
