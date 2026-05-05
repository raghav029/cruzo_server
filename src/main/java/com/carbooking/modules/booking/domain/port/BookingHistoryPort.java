package com.carbooking.modules.booking.domain.port;

import com.carbooking.entity.Booking;
import com.carbooking.entity.BookingStatusHistory;

import java.util.List;

public interface BookingHistoryPort {
    List<BookingStatusHistory> findByBookingOrderByTransitionedAtAsc(Booking booking);
    BookingStatusHistory save(BookingStatusHistory history);
}
