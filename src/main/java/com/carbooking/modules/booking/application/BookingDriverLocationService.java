package com.carbooking.modules.booking.application;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Booking;
import com.carbooking.entity.Driver;
import com.carbooking.modules.booking.dto.request.UpdateDriverLocationRequest;
import com.carbooking.modules.booking.dto.response.BookingResponse;
import com.carbooking.modules.booking.application.BookingMapper;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.fleet.domain.port.DriverPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class BookingDriverLocationService extends TenantSupport {

    private final BookingPort bookingRepository;
    private final DriverPort driverRepository;
    private final BookingMapper bookingMapper;

    public BookingDriverLocationService(
            BookingPort bookingRepository,
            DriverPort driverRepository,
            BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.driverRepository = driverRepository;
        this.bookingMapper = bookingMapper;
    }

    @Transactional
    public BookingResponse updateDriverLocation(UUID bookingId, UpdateDriverLocationRequest request) {
        Driver driver = driverRepository.findByUser(currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getDriver() == null || !booking.getDriver().getId().equals(driver.getId())) {
            throw new UnauthorizedException("You are not assigned to this booking");
        }

        if (booking.getStatus() != BookingStatus.DRIVER_EN_ROUTE
                && booking.getStatus() != BookingStatus.ARRIVED
                && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Location updates only allowed during active trip");
        }

        booking.setDriverCurrentLat(request.getLat());
        booking.setDriverCurrentLng(request.getLng());
        booking.setLocationUpdatedAt(Instant.now());
        bookingRepository.save(booking);

        return bookingMapper.toResponse(booking);
    }
}
