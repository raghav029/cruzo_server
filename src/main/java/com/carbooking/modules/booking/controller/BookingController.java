package com.carbooking.modules.booking.controller;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.booking.application.BookingService;
import com.carbooking.modules.booking.dto.request.AssignDriverRequest;
import com.carbooking.modules.booking.dto.request.CancelBookingRequest;
import com.carbooking.modules.booking.dto.request.CreateBookingRequest;
import com.carbooking.modules.booking.dto.request.RejectBookingRequest;
import com.carbooking.modules.booking.dto.request.UpdateDriverLocationRequest;
import com.carbooking.modules.booking.dto.request.VerifyOtpRequest;
import com.carbooking.modules.booking.dto.response.BookingResponse;
import com.carbooking.modules.booking.dto.response.BookingStatusHistoryResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Bookings")
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CORPORATE_ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> create(
            @Valid @RequestBody CreateBookingRequest request) {
        return ResponseHelper.created(bookingService.create(request));
    }

    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'CORPORATE_ADMIN', 'EMPLOYEE', 'DRIVER')")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> list(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseHelper.ok(new PagedResponse<>(
                bookingService.list(status, fromDate, toDate, PageRequest.of(page, size))));
    }

    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'CORPORATE_ADMIN', 'EMPLOYEE', 'DRIVER')")
    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> get(@PathVariable UUID bookingId) {
        return ResponseHelper.ok(bookingService.get(bookingId));
    }

    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'CORPORATE_ADMIN', 'EMPLOYEE')")
    @GetMapping("/{bookingId}/history")
    public ResponseEntity<ApiResponse<List<BookingStatusHistoryResponse>>> getHistory(
            @PathVariable UUID bookingId) {
        return ResponseHelper.ok(bookingService.getHistory(bookingId));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/my-trip")
    public ResponseEntity<ApiResponse<BookingResponse>> getMyActiveTrip() {
        return ResponseHelper.ok(bookingService.getMyActiveTrip());
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @PostMapping("/{bookingId}/approve")
    public ResponseEntity<ApiResponse<BookingResponse>> approve(@PathVariable UUID bookingId) {
        return ResponseHelper.ok(bookingService.approve(bookingId));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @PostMapping("/{bookingId}/reject")
    public ResponseEntity<ApiResponse<BookingResponse>> reject(
            @PathVariable UUID bookingId,
            @RequestBody(required = false) RejectBookingRequest request) {
        return ResponseHelper.ok(bookingService.reject(bookingId, request != null ? request : new RejectBookingRequest()));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @PostMapping("/{bookingId}/assign-driver")
    public ResponseEntity<ApiResponse<BookingResponse>> assignDriver(
            @PathVariable UUID bookingId,
            @Valid @RequestBody AssignDriverRequest request) {
        return ResponseHelper.ok(bookingService.assignDriver(bookingId, request));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @PostMapping("/{bookingId}/auto-assign")
    public ResponseEntity<ApiResponse<BookingResponse>> autoAssign(@PathVariable UUID bookingId) {
        return ResponseHelper.ok(bookingService.autoAssign(bookingId));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PatchMapping("/{bookingId}/location")
    public ResponseEntity<ApiResponse<BookingResponse>> updateDriverLocation(
            @PathVariable UUID bookingId,
            @Valid @RequestBody UpdateDriverLocationRequest request) {
        return ResponseHelper.ok(bookingService.updateDriverLocation(bookingId, request));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{bookingId}/status")
    public ResponseEntity<ApiResponse<BookingResponse>> updateDriverStatus(
            @PathVariable UUID bookingId,
            @RequestParam String to) {
        return ResponseHelper.ok(bookingService.updateDriverStatus(bookingId, to));
    }

    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'CORPORATE_ADMIN', 'EMPLOYEE', 'DRIVER')")
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(
            @PathVariable UUID bookingId,
            @RequestBody(required = false) CancelBookingRequest request) {
        return ResponseHelper.ok(bookingService.cancel(bookingId, request != null ? request : new CancelBookingRequest()));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{bookingId}/verify-otp")
    public ResponseEntity<ApiResponse<BookingResponse>> verifyOtp(
            @PathVariable UUID bookingId,
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseHelper.ok(bookingService.verifyOtp(bookingId, request));
    }
}
