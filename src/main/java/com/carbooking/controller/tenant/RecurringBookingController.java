package com.carbooking.controller.tenant;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.dto.request.recurring.CreateRecurringBookingRequest;
import com.carbooking.dto.response.recurring.RecurringBookingResponse;
import com.carbooking.service.RecurringBookingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recurring-bookings")
@RequiredArgsConstructor
@Tag(name = "Recurring Bookings")
public class RecurringBookingController {

    private final RecurringBookingService recurringBookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<RecurringBookingResponse>> create(
            @Valid @RequestBody CreateRecurringBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(recurringBookingService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecurringBookingResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(recurringBookingService.list()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringBookingResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(recurringBookingService.get(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringBookingResponse>> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(recurringBookingService.deactivate(id)));
    }
}
