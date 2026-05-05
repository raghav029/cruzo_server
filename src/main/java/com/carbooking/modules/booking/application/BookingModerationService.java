package com.carbooking.modules.booking.application;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Booking;
import com.carbooking.modules.booking.dto.request.RejectBookingRequest;
import com.carbooking.modules.booking.dto.response.BookingResponse;
import com.carbooking.modules.booking.application.BookingMapper;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.notification.application.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class BookingModerationService extends TenantSupport {

    private final BookingPort bookingRepository;
    private final BookingHistoryHelper historyHelper;
    private final NotificationService notificationService;
    private final BookingMapper bookingMapper;

    public BookingModerationService(
            BookingPort bookingRepository,
            BookingHistoryHelper historyHelper,
            NotificationService notificationService,
            BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.historyHelper = historyHelper;
        this.notificationService = notificationService;
        this.bookingMapper = bookingMapper;
    }

    @Transactional
    public BookingResponse approve(UUID bookingId) {
        Booking booking = findAndVerify(bookingId);
        requireStatus(booking, BookingStatus.PENDING_APPROVAL);

        booking.setStatus(BookingStatus.APPROVED);
        booking.setApprovedAt(Instant.now());
        bookingRepository.save(booking);
        historyHelper.append(booking, BookingStatus.PENDING_APPROVAL, BookingStatus.APPROVED, currentUser(), null);
        notificationService.notifyBookingApproved(booking);

        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public BookingResponse reject(UUID bookingId, RejectBookingRequest request) {
        Booking booking = findAndVerify(bookingId);
        requireStatus(booking, BookingStatus.PENDING_APPROVAL);

        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectionReason(request.getReason());
        bookingRepository.save(booking);
        historyHelper.append(booking, BookingStatus.PENDING_APPROVAL, BookingStatus.REJECTED, currentUser(), request.getReason());
        notificationService.notifyBookingRejected(booking);

        return bookingMapper.toResponse(booking);
    }

    private void requireStatus(Booking booking, BookingStatus required) {
        if (booking.getStatus() != required) {
            throw new BusinessRuleException("Booking must be in " + required + " status, current: " + booking.getStatus());
        }
    }

    private Booking findAndVerify(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        assertSameTenant(booking.getTenant().getId());
        UUID userId = currentUserId();
        String role = currentRole();
        if ("ROLE_EMPLOYEE".equals(role) && !booking.getEmployee().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
        return booking;
    }
}
