package com.carbooking.modules.fleet.application;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.DuplicateResourceException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.Vehicle;
import com.carbooking.modules.fleet.dto.request.CreateVehicleRequest;
import com.carbooking.modules.fleet.dto.request.UpdateVehicleRequest;
import com.carbooking.modules.fleet.dto.response.VehicleResponse;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.fleet.domain.port.VehiclePort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import com.carbooking.modules.fleet.application.VehicleMapper;

@Service
public class VehicleService extends TenantSupport {

    private final VehicleMapper vehicleMapper;
    private final VehiclePort vehicleRepository;
    private final BookingPort bookingRepository;

    public VehicleService(VehiclePort vehicleRepository, BookingPort bookingRepository,
                                 VehicleMapper vehicleMapper) {
        this.vehicleRepository = vehicleRepository;
        this.bookingRepository = bookingRepository;
        this.vehicleMapper = vehicleMapper;
    }

    @Transactional
    public VehicleResponse create(CreateVehicleRequest request) {
        Tenant tenant = requireTenant();

        if (vehicleRepository.existsByTenantAndPlateNumber(tenant, request.getPlateNumber())) {
            throw new DuplicateResourceException(
                    "Vehicle with plate number '" + request.getPlateNumber() + "' already exists");
        }

        Vehicle vehicle = Vehicle.builder()
                .tenant(tenant)
                .plateNumber(request.getPlateNumber().toUpperCase())
                .vehicleType(request.getVehicleType())
                .make(request.getMake())
                .model(request.getModel())
                .year(request.getYear())
                .color(request.getColor())
                .insuranceExpiry(request.getInsuranceExpiry())
                .fitnessExpiry(request.getFitnessExpiry())
                .status(VehicleStatus.ACTIVE)
                .build();

        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional(readOnly = true)
    public Page<VehicleResponse> list(VehicleStatus status, Pageable pageable) {
        Tenant tenant = requireTenant();
        Page<Vehicle> page = (status != null)
                ? vehicleRepository.findByTenantAndStatus(tenant, status, pageable)
                : vehicleRepository.findByTenant(tenant, pageable);
        return page.map(vehicleMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public VehicleResponse get(UUID vehicleId) {
        return vehicleMapper.toResponse(findAndVerify(vehicleId));
    }

    @Transactional
    public VehicleResponse update(UUID vehicleId, UpdateVehicleRequest request) {
        Vehicle vehicle = findAndVerify(vehicleId);

        if (request.getVehicleType() != null)    vehicle.setVehicleType(request.getVehicleType());
        if (request.getMake() != null)           vehicle.setMake(request.getMake());
        if (request.getModel() != null)          vehicle.setModel(request.getModel());
        if (request.getYear() != null)           vehicle.setYear(request.getYear());
        if (request.getColor() != null)          vehicle.setColor(request.getColor());
        if (request.getStatus() != null)         vehicle.setStatus(request.getStatus());
        if (request.getInsuranceExpiry() != null) vehicle.setInsuranceExpiry(request.getInsuranceExpiry());
        if (request.getFitnessExpiry() != null)  vehicle.setFitnessExpiry(request.getFitnessExpiry());

        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void delete(UUID vehicleId) {
        Vehicle vehicle = findAndVerify(vehicleId);

        boolean hasActiveBooking = bookingRepository
                .findByTenant(vehicle.getTenant(), Pageable.unpaged())
                .stream()
                .anyMatch(b -> b.getVehicle() != null
                        && b.getVehicle().getId().equals(vehicleId)
                        && isActiveBookingStatus(b.getStatus().name()));

        if (hasActiveBooking) {
            throw new BusinessRuleException("Cannot deactivate vehicle with an active booking");
        }

        vehicle.setStatus(VehicleStatus.INACTIVE);
        vehicleRepository.save(vehicle);
    }

    private Vehicle findAndVerify(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
        assertSameTenant(vehicle.getTenant().getId());
        return vehicle;
    }

    private boolean isActiveBookingStatus(String status) {
        return switch (status) {
            case "PENDING_APPROVAL", "APPROVED", "DRIVER_ASSIGNED",
                 "DRIVER_EN_ROUTE", "ARRIVED", "IN_PROGRESS" -> true;
            default -> false;
        };
    }

}
