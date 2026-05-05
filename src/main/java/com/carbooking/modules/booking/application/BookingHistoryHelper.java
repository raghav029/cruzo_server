package com.carbooking.modules.booking.application;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.entity.Booking;
import com.carbooking.entity.BookingStatusHistory;
import com.carbooking.entity.User;
import com.carbooking.modules.booking.domain.port.BookingHistoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Appends a BookingStatusHistory row for every booking status transition.
 *
 * Every booking state change MUST call this helper in the same @Transactional method.
 * Never copy the builder inline — always delegate here.
 *
 * Usage:
 * <pre>
 *   historyHelper.append(booking, oldStatus, BookingStatus.APPROVED, actor, null);
 *   historyHelper.append(booking, oldStatus, BookingStatus.REJECTED, actor, "Insufficient capacity");
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class BookingHistoryHelper {

    private final BookingHistoryPort historyRepository;

    /**
     * Records a status transition. Pass null for reason when none applies.
     */
    public void append(Booking booking, BookingStatus from, BookingStatus to, User actor, String reason) {
        historyRepository.save(BookingStatusHistory.builder()
                .tenant(booking.getTenant())
                .booking(booking)
                .fromStatus(from)
                .toStatus(to)
                .actor(actor)
                .reason(reason)
                .transitionedAt(Instant.now())
                .build());
    }
}
