package com.carbooking.modules.booking.infrastructure.persistence;

import com.carbooking.entity.Booking;
import com.carbooking.entity.BookingStatusHistory;
import com.carbooking.modules.booking.domain.port.BookingHistoryPort;
import com.carbooking.repository.BookingStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingHistoryJpaAdapter implements BookingHistoryPort {

    private final BookingStatusHistoryRepository repo;

    @Override public List<BookingStatusHistory> findByBookingOrderByTransitionedAtAsc(Booking booking) { return repo.findByBookingOrderByTransitionedAtAsc(booking); }
    @Override public BookingStatusHistory save(BookingStatusHistory history) { return repo.save(history); }
}
