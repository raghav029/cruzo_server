package com.carbooking.modules.addon.domain.port;

import com.carbooking.entity.BookingAddon;
import com.carbooking.entity.BookingAddonId;

import java.util.List;
import java.util.UUID;

public interface BookingAddonPort {
    BookingAddon save(BookingAddon bookingAddon);
    List<BookingAddon> findByBookingId(UUID bookingId);
    void deleteByBookingId(UUID bookingId);
}
