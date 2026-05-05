package com.carbooking.repository;

import com.carbooking.entity.Booking;
import com.carbooking.entity.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, UUID> {
    List<BookingStatusHistory> findByBookingOrderByTransitionedAtAsc(Booking booking);
}
