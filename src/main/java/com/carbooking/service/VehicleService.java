package com.carbooking.service;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.DuplicateResourceException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.request.vehicle.CreateVehicleRequest;
import com.carbooking.dto.request.vehicle.UpdateVehicleRequest;
import com.carbooking.dto.response.vehicle.VehicleResponse;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.Vehicle;
import com.carbooking.repository.BookingRepository;
import com.carbooking.repository.TenantRepository;
import com.carbooking.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final TenantRepository tenantRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public VehicleResponse create(CreateVehicleRequest request) {
        Tenant tenant = currentTenant();

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

        return toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional(readOnly = true)
    public Page<VehicleResponse> list(VehicleStatus status, Pageable pageable) {
        Tenant tenant = currentTenant();
        Page<Vehicle> page = (status != null)
                ? vehicleRepository.findByTenantAndStatus(tenant, status, pageable)
                : vehicleRepository.findByTenant(tenant, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public VehicleResponse get(UUID vehicleId) {
        Vehicle vehicle = findAndVerify(vehicleId);
        return toResponse(vehicle);
    }

    @Transactional
    public VehicleResponse update(UUID vehicleId, UpdateVehicleRequest request) {
        Vehicle vehicle = findAndVerify(vehicleId);

        if (request.getVehicleType() != null) vehicle.setVehicleType(request.getVehicleType());
        if (request.getMake() != null) vehicle.setMake(request.getMake());
        if (request.getModel() != null) vehicle.setModel(request.getModel());
        if (request.getYear() != null) vehicle.setYear(request.getYear());
        if (request.getColor() != null) vehicle.setColor(request.getColor());
        if (request.getStatus() != null) vehicle.setStatus(request.getStatus());
        if (request.getInsuranceExpiry() != null) vehicle.setInsuranceExpiry(request.getInsuranceExpiry());
        if (request.getFitnessExpiry() != null) vehicle.setFitnessExpiry(request.getFitnessExpiry());

        return toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void delete(UUID vehicleId) {
        Vehicle vehicle = findAndVerify(vehicleId);

        long activeBookings = bookingRepository.countActiveByTenant(vehicle.getTenant());
        // check specifically this vehicle has no active booking
        boolean hasActiveBooking = bookingRepository
                .findByTenant(vehicle.getTenant(), Pageable.unpaged())
                .stream()
                .anyMatch(b -> b.getVehicle() != null
                        && b.getVehicle().getId().equals(vehicleId)
                        && isActiveStatus(b.getStatus().name()));

        if (hasActiveBooking) {
            throw new BusinessRuleException("Cannot deactivate vehicle with an active booking");
        }

        vehicle.setStatus(VehicleStatus.INACTIVE);
        vehicleRepository.save(vehicle);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Vehicle findAndVerify(UUID vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
        if (!vehicle.getTenant().getId().equals(SecurityUtils.getCurrentTenantId())) {
            throw new UnauthorizedException("Access denied");
        }
        return vehicle;
    }

    private Tenant currentTenant() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    private boolean isActiveStatus(String status) {
        return switch (status) {
            case "PENDING_APPROVAL", "APPROVED", "DRIVER_ASSIGNED",
                 "DRIVER_EN_ROUTE", "ARRIVED", "IN_PROGRESS" -> true;
            default -> false;
        };
    }

    private VehicleResponse toResponse(Vehicle v) {
        return VehicleResponse.builder()
                .id(v.getId())
                .tenantId(v.getTenant().getId())
                .plateNumber(v.getPlateNumber())
                .vehicleType(v.getVehicleType())
                .make(v.getMake())
                .model(v.getModel())
                .year(v.getYear())
                .color(v.getColor())
                .status(v.getStatus())
                .insuranceExpiry(v.getInsuranceExpiry())
                .fitnessExpiry(v.getFitnessExpiry())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}
