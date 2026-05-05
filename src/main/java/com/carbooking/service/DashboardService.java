package com.carbooking.service;

import com.carbooking.common.enums.*;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.response.dashboard.DashboardSummaryResponse;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.Vehicle;
import com.carbooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TenantRepository tenantRepo;
    private final BookingRepository bookingRepo;
    private final VehicleRepository vehicleRepo;
    private final DriverRepository driverRepo;
    private final InvoiceRepository invoiceRepo;
    private final SosAlertRepository sosRepo;
    private final DailyTripRepository dailyTripRepo;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        ZoneId zone = ZoneId.of(tenant.getTimezone() != null ? tenant.getTimezone() : "Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);
        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();

        // Vehicles
        long totalVehicles = vehicleRepo.countByTenant(tenant);
        long vehiclesInTrip = vehicleRepo.countByTenantAndStatus(tenant, VehicleStatus.IN_TRIP);

        // Drivers
        long totalDrivers = driverRepo.countByTenant(tenant);
        long availableDrivers = driverRepo.countByTenantAndAvailability(tenant, DriverAvailability.AVAILABLE);

        // Bookings
        long pendingApprovals = bookingRepo.countByTenantAndStatus(tenant, BookingStatus.PENDING_APPROVAL);
        long activeTrips = bookingRepo.countByTenantAndStatus(tenant, BookingStatus.IN_PROGRESS);
        long totalBookingsThisMonth = bookingRepo.countByTenantAndCreatedAtBetween(tenant, startOfMonth, endOfDay);
        BigDecimal revenueThisMonth = bookingRepo.sumFinalFareByTenantAndStatusAndTripCompletedAtBetween(
                tenant, BookingStatus.COMPLETED, startOfMonth, endOfDay);
        if (revenueThisMonth == null) revenueThisMonth = BigDecimal.ZERO;

        // Daily trips today
        long tripsToday = dailyTripRepo.countByTenantAndTripDate(tenant, today);
        long unassignedTrips = dailyTripRepo.countByTenantAndTripDateAndDriverIsNull(tenant, today);

        // SOS
        long activeSosAlerts = sosRepo.countByTenantAndStatus(tenant, SosStatus.ACTIVE);

        // Pending invoices
        long pendingInvoices = invoiceRepo.countByTenantAndStatus(tenant, InvoiceStatus.DRAFT)
                + invoiceRepo.countByTenantAndStatus(tenant, InvoiceStatus.SENT);

        // Expiring documents (next 30 days)
        LocalDate in30Days = today.plusDays(30);
        List<Driver> expiringDriverDocs = driverRepo.findByTenantAndLicenseExpiryBefore(tenant, in30Days);
        List<Vehicle> expiringVehicleDocs = vehicleRepo.findByTenantAndInsuranceExpiryBeforeAndStatusNot(
                tenant, in30Days, VehicleStatus.INACTIVE);
        long expiringDocuments = expiringDriverDocs.size() + expiringVehicleDocs.size();

        return DashboardSummaryResponse.builder()
                .tripsToday(tripsToday)
                .activeTrips(activeTrips)
                .pendingApprovals(pendingApprovals)
                .unassignedTrips(unassignedTrips)
                .totalVehicles(totalVehicles)
                .vehiclesInTrip(vehiclesInTrip)
                .totalDrivers(totalDrivers)
                .availableDrivers(availableDrivers)
                .pendingInvoices(pendingInvoices)
                .activeSosAlerts(activeSosAlerts)
                .expiringDocuments(expiringDocuments)
                .totalBookingsThisMonth(totalBookingsThisMonth)
                .revenueThisMonth(revenueThisMonth)
                .build();
    }
}
