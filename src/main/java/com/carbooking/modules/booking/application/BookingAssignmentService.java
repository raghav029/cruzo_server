package com.carbooking.modules.booking.application;

import com.carbooking.common.enums.AssignmentMode;
import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.enums.DriverAvailability;
import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Booking;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.Vehicle;
import com.carbooking.modules.booking.dto.request.AssignDriverRequest;
import com.carbooking.modules.booking.dto.response.BookingResponse;
import com.carbooking.modules.booking.application.BookingMapper;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.fleet.domain.port.DriverPort;
import com.carbooking.modules.fleet.domain.port.VehiclePort;
import com.carbooking.modules.notification.application.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BookingAssignmentService extends TenantSupport {

    private final BookingPort bookingRepository;
    private final DriverPort driverRepository;
    private final VehiclePort vehicleRepository;
    private final BookingHistoryHelper historyHelper;
    private final NotificationService notificationService;
    private final BookingMapper bookingMapper;

    public BookingAssignmentService(
            BookingPort bookingRepository,
            DriverPort driverRepository,
            VehiclePort vehicleRepository,
            BookingHistoryHelper historyHelper,
            NotificationService notificationService,
            BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.historyHelper = historyHelper;
        this.notificationService = notificationService;
        this.bookingMapper = bookingMapper;
    }

    @Transactional
    public BookingResponse assignDriver(UUID bookingId, AssignDriverRequest request) {
        Booking booking = findAndVerify(bookingId);
        requireStatus(booking, BookingStatus.APPROVED);

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        assertSameTenant(driver.getTenant().getId());
        if (driver.getAvailability() != DriverAvailability.AVAILABLE) {
            throw new BusinessRuleException("Driver is not available");
        }

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        assertSameTenant(vehicle.getTenant().getId());
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new BusinessRuleException("Vehicle is not active");
        }

        doAssign(booking, driver, vehicle, AssignmentMode.MANUAL);
        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public BookingResponse autoAssign(UUID bookingId) {
        Booking booking = findAndVerify(bookingId);
        requireStatus(booking, BookingStatus.APPROVED);

        Tenant tenant = requireTenant();

        List<Driver> availableDrivers = driverRepository.findAvailableDriversByTenantOrderByLastUpdated(tenant);
        if (availableDrivers.isEmpty()) throw new BusinessRuleException("No available drivers");

        List<Vehicle> vehicles = vehicleRepository.findByTenantAndStatusAndVehicleType(
                tenant, VehicleStatus.ACTIVE, booking.getVehicleTypeRequested());
        if (vehicles.isEmpty()) throw new BusinessRuleException("No active vehicles of type " + booking.getVehicleTypeRequested());

        doAssign(booking, availableDrivers.get(0), vehicles.get(0), AssignmentMode.AUTO);
        return bookingMapper.toResponse(booking);
    }

    private void doAssign(Booking booking, Driver driver, Vehicle vehicle, AssignmentMode mode) {
        BookingStatus from = booking.getStatus();
        booking.setDriver(driver);
        booking.setVehicle(vehicle);
        booking.setAssignedBy(currentUser());
        booking.setAssignmentMode(mode);
        booking.setStatus(BookingStatus.DRIVER_ASSIGNED);
        booking.setDriverAssignedAt(Instant.now());

        driver.setAvailability(DriverAvailability.ON_TRIP);
        vehicle.setStatus(VehicleStatus.IN_TRIP);
        driverRepository.save(driver);
        vehicleRepository.save(vehicle);
        bookingRepository.save(booking);
        historyHelper.append(booking, from, BookingStatus.DRIVER_ASSIGNED, currentUser(), null);
        notificationService.notifyDriverAssigned(booking);
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
        return booking;
    }
}
