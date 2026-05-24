package com.carbooking.modules.addon.infrastructure;

import com.carbooking.entity.BookingAddon;
import com.carbooking.entity.BookingAddonId;
import com.carbooking.modules.addon.domain.port.BookingAddonPort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingAddonRepository extends JpaRepository<BookingAddon, BookingAddonId>, BookingAddonPort {
    List<BookingAddon> findByBookingId(UUID bookingId);
    void deleteByBookingId(UUID bookingId);
}
