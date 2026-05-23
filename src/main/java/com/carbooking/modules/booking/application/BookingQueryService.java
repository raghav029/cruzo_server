package com.carbooking.modules.booking.application;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Booking;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.modules.booking.dto.response.BookingResponse;
import com.carbooking.modules.booking.dto.response.BookingStatusHistoryResponse;
import com.carbooking.modules.booking.dto.response.LiveTripResponse;
import com.carbooking.modules.booking.application.BookingMapper;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.booking.domain.port.BookingHistoryPort;
import com.carbooking.modules.fleet.domain.port.DriverPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BookingQueryService extends TenantSupport {

    private final BookingPort bookingRepository;
    private final BookingHistoryPort historyRepository;
    private final DriverPort driverRepository;
    private final BookingMapper bookingMapper;

    public BookingQueryService(
            BookingPort bookingRepository,
            BookingHistoryPort historyRepository,
            DriverPort driverRepository,
            BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.historyRepository = historyRepository;
        this.driverRepository = driverRepository;
        this.bookingMapper = bookingMapper;
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> list(BookingStatus status, Instant fromDate, Instant toDate, Pageable pageable) {
        Tenant tenant = requireTenant();
        String role = currentRole();
        UUID userId = currentUserId();

        Page<Booking> page;
        if ("ROLE_EMPLOYEE".equals(role)) {
            User employee = currentUser();
            Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
            page = bookingRepository.findByEmployee(employee, sorted);
        } else if ("ROLE_DRIVER".equals(role)) {
            Driver driver = driverRepository.findByUser(currentUser())
                    .orElseThrow(() -> new com.carbooking.common.exception.ResourceNotFoundException("Driver profile not found"));
            page = status != null
                    ? bookingRepository.findByDriverAndStatus(driver, status, pageable)
                    : bookingRepository.findByDriver(driver, pageable);
        } else if (fromDate != null && toDate != null && status != null) {
            page = bookingRepository.findByTenantAndStatusAndScheduledAtBetween(tenant, status, fromDate, toDate, pageable);
        } else if (fromDate != null && toDate != null) {
            page = bookingRepository.findByTenantAndScheduledAtBetween(tenant, fromDate, toDate, pageable);
        } else if (status != null) {
            page = bookingRepository.findByTenantAndStatus(tenant, status, pageable);
        } else {
            page = bookingRepository.findByTenant(tenant, pageable);
        }
        return page.map(bookingMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse get(UUID bookingId) {
        return bookingMapper.toResponse(findAndVerify(bookingId));
    }

    @Transactional(readOnly = true)
    public List<BookingStatusHistoryResponse> getHistory(UUID bookingId) {
        return historyRepository.findByBookingOrderByTransitionedAtAsc(findAndVerify(bookingId))
                .stream()
                .map(bookingMapper::toHistoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getMyActiveTrip() {
        Driver driver = driverRepository.findByUser(currentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
        return bookingRepository.findFirstByDriverAndStatusIn(driver, List.of(
                BookingStatus.DRIVER_ASSIGNED, BookingStatus.DRIVER_EN_ROUTE,
                BookingStatus.ARRIVED, BookingStatus.IN_PROGRESS))
                .map(bookingMapper::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> myActive() {
        User employee = currentUser();
        List<BookingStatus> activeStatuses = List.of(
            BookingStatus.DRIVER_ASSIGNED,
            BookingStatus.DRIVER_EN_ROUTE,
            BookingStatus.ARRIVED,
            BookingStatus.IN_PROGRESS
        );
        return bookingRepository.findByEmployeeAndStatusIn(employee, activeStatuses)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LiveTripResponse> getLiveTrips() {
        Tenant tenant = requireTenant();
        return bookingRepository.findByTenantAndStatus(tenant, BookingStatus.IN_PROGRESS)
                .stream()
                .map(this::toLiveTripResponse)
                .toList();
    }

    private LiveTripResponse toLiveTripResponse(Booking b) {
        String passengerName = b.getEmployee() != null
                ? b.getEmployee().getFullName()
                : (b.getCustomer() != null ? b.getCustomer().getName() : "Unknown");

        String vehicleName = b.getVehicle() != null
                ? b.getVehicle().getMake() + " " + b.getVehicle().getModel()
                : "Unknown Vehicle";

        String driverName = b.getDriver() != null && b.getDriver().getUser() != null
                ? b.getDriver().getUser().getFullName()
                : null;

        String driverPhone = b.getDriver() != null && b.getDriver().getUser() != null
                ? b.getDriver().getUser().getPhone()
                : null;

        return LiveTripResponse.builder()
                .bookingId(b.getId())
                .vehicleName(vehicleName)
                .plateNumber(b.getVehicle() != null ? b.getVehicle().getPlateNumber() : null)
                .driverName(driverName)
                .driverPhone(driverPhone)
                .passengerName(passengerName)
                .pickupAddress(b.getPickupAddress())
                .dropAddress(b.getDropAddress())
                .driverLat(b.getDriverCurrentLat())
                .driverLng(b.getDriverCurrentLng())
                .locationUpdatedAt(b.getLocationUpdatedAt())
                .scheduledAt(b.getScheduledAt())
                .build();
    }

    private Booking findAndVerify(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        assertSameTenant(booking.getTenant().getId());
        String role = currentRole();
        UUID userId = currentUserId();
        if ("ROLE_EMPLOYEE".equals(role) && !booking.getEmployee().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
        return booking;
    }
}
