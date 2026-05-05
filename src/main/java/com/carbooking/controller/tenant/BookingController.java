package com.carbooking.controller.tenant;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.dto.request.booking.*;
import com.carbooking.dto.response.booking.BookingResponse;
import com.carbooking.dto.response.booking.BookingStatusHistoryResponse;
import com.carbooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Bookings")
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> create(
            @Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(bookingService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> list(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                new PagedResponse<>(bookingService.list(status, PageRequest.of(page, size)))));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> get(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.get(bookingId)));
    }

    @GetMapping("/{bookingId}/history")
    public ResponseEntity<ApiResponse<List<BookingStatusHistoryResponse>>> getHistory(
            @PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getHistory(bookingId)));
    }

    @GetMapping("/my-trip")
    public ResponseEntity<ApiResponse<BookingResponse>> getMyActiveTrip() {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getMyActiveTrip()));
    }

    @PostMapping("/{bookingId}/approve")
    public ResponseEntity<ApiResponse<BookingResponse>> approve(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.approve(bookingId)));
    }

    @PostMapping("/{bookingId}/reject")
    public ResponseEntity<ApiResponse<BookingResponse>> reject(
            @PathVariable UUID bookingId,
            @RequestBody(required = false) RejectBookingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.reject(bookingId, request != null ? request : new RejectBookingRequest())));
    }

    @PostMapping("/{bookingId}/assign-driver")
    public ResponseEntity<ApiResponse<BookingResponse>> assignDriver(
            @PathVariable UUID bookingId,
            @Valid @RequestBody AssignDriverRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.assignDriver(bookingId, request)));
    }

    @PostMapping("/{bookingId}/auto-assign")
    public ResponseEntity<ApiResponse<BookingResponse>> autoAssign(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.autoAssign(bookingId)));
    }

    @PatchMapping("/{bookingId}/location")
    public ResponseEntity<ApiResponse<BookingResponse>> updateDriverLocation(
            @PathVariable UUID bookingId,
            @Valid @RequestBody UpdateDriverLocationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.updateDriverLocation(bookingId, request)));
    }

    @PostMapping("/{bookingId}/status")
    public ResponseEntity<ApiResponse<BookingResponse>> updateDriverStatus(
            @PathVariable UUID bookingId,
            @RequestParam String to) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.updateDriverStatus(bookingId, to)));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(
            @PathVariable UUID bookingId,
            @RequestBody(required = false) CancelBookingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.cancel(bookingId, request != null ? request : new CancelBookingRequest())));
    }

    @PostMapping("/{bookingId}/verify-otp")
    public ResponseEntity<ApiResponse<BookingResponse>> verifyOtp(
            @PathVariable UUID bookingId,
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.verifyOtp(bookingId, request)));
    }
}
