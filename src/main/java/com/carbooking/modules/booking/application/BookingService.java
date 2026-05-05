package com.carbooking.modules.booking.application;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.modules.booking.dto.request.AssignDriverRequest;
import com.carbooking.modules.booking.dto.request.CancelBookingRequest;
import com.carbooking.modules.booking.dto.request.CreateBookingRequest;
import com.carbooking.modules.booking.dto.request.RejectBookingRequest;
import com.carbooking.modules.booking.dto.request.UpdateDriverLocationRequest;
import com.carbooking.modules.booking.dto.request.VerifyOtpRequest;
import com.carbooking.modules.booking.dto.response.BookingResponse;
import com.carbooking.modules.booking.dto.response.BookingStatusHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingCreationService bookingCreationService;
    private final BookingModerationService bookingModerationService;
    private final BookingAssignmentService bookingAssignmentService;
    private final BookingDriverLocationService bookingDriverLocationService;
    private final BookingDriverStatusService bookingDriverStatusService;
    private final BookingOtpVerificationService bookingOtpVerificationService;
    private final BookingCancellationService bookingCancellationService;
    private final BookingQueryService bookingQueryService;

    @Transactional
    public BookingResponse create(CreateBookingRequest request) {
        return bookingCreationService.create(request);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> list(BookingStatus status, Instant fromDate, Instant toDate, Pageable pageable) {
        return bookingQueryService.list(status, fromDate, toDate, pageable);
    }

    @Transactional(readOnly = true)
    public BookingResponse get(UUID bookingId) {
        return bookingQueryService.get(bookingId);
    }

    @Transactional(readOnly = true)
    public List<BookingStatusHistoryResponse> getHistory(UUID bookingId) {
        return bookingQueryService.getHistory(bookingId);
    }

    @Transactional(readOnly = true)
    public BookingResponse getMyActiveTrip() {
        return bookingQueryService.getMyActiveTrip();
    }

    @Transactional
    public BookingResponse updateDriverLocation(UUID bookingId, UpdateDriverLocationRequest request) {
        return bookingDriverLocationService.updateDriverLocation(bookingId, request);
    }

    @Transactional
    public BookingResponse approve(UUID bookingId) {
        return bookingModerationService.approve(bookingId);
    }

    @Transactional
    public BookingResponse reject(UUID bookingId, RejectBookingRequest request) {
        return bookingModerationService.reject(bookingId, request);
    }

    @Transactional
    public BookingResponse assignDriver(UUID bookingId, AssignDriverRequest request) {
        return bookingAssignmentService.assignDriver(bookingId, request);
    }

    @Transactional
    public BookingResponse autoAssign(UUID bookingId) {
        return bookingAssignmentService.autoAssign(bookingId);
    }

    @Transactional
    public BookingResponse updateDriverStatus(UUID bookingId, String targetStatus) {
        return bookingDriverStatusService.updateDriverStatus(bookingId, targetStatus);
    }

    @Transactional
    public BookingResponse verifyOtp(UUID bookingId, VerifyOtpRequest request) {
        return bookingOtpVerificationService.verifyOtp(bookingId, request);
    }

    @Transactional
    public BookingResponse cancel(UUID bookingId, CancelBookingRequest request) {
        return bookingCancellationService.cancel(bookingId, request);
    }
}
