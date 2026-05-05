package com.carbooking.modules.dashboard.application;

import com.carbooking.common.enums.*;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.Vehicle;
import com.carbooking.modules.dashboard.dto.response.DashboardSummaryResponse;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.dailyschedule.domain.port.DailyTripPort;
import com.carbooking.modules.fleet.domain.port.DriverPort;
import com.carbooking.modules.fleet.domain.port.VehiclePort;
import com.carbooking.modules.invoice.domain.port.InvoicePort;
import com.carbooking.modules.sos.domain.port.SosAlertPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Service
public class DashboardService extends TenantSupport {

    private final BookingPort bookingRepo;
    private final VehiclePort vehicleRepo;
    private final DriverPort driverRepo;
    private final InvoicePort invoiceRepo;
    private final SosAlertPort sosRepo;
    private final DailyTripPort dailyTripRepo;

    public DashboardService(BookingPort bookingRepo, VehiclePort vehicleRepo,
                            DriverPort driverRepo, InvoicePort invoiceRepo,
                            SosAlertPort sosRepo, DailyTripPort dailyTripRepo) {
        this.bookingRepo = bookingRepo;
        this.vehicleRepo = vehicleRepo;
        this.driverRepo = driverRepo;
        this.invoiceRepo = invoiceRepo;
        this.sosRepo = sosRepo;
        this.dailyTripRepo = dailyTripRepo;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        Tenant tenant = requireTenant();

        ZoneId zone = ZoneId.of(tenant.getTimezone() != null ? tenant.getTimezone() : "Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);
        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();

        long totalVehicles = vehicleRepo.countByTenant(tenant);
        long vehiclesInTrip = vehicleRepo.countByTenantAndStatus(tenant, VehicleStatus.IN_TRIP);
        long totalDrivers = driverRepo.countByTenant(tenant);
        long availableDrivers = driverRepo.countByTenantAndAvailability(tenant, DriverAvailability.AVAILABLE);
        long pendingApprovals = bookingRepo.countByTenantAndStatus(tenant, BookingStatus.PENDING_APPROVAL);
        long activeTrips = bookingRepo.countByTenantAndStatus(tenant, BookingStatus.IN_PROGRESS);
        long totalBookingsThisMonth = bookingRepo.countByTenantAndCreatedAtBetween(tenant, startOfMonth, endOfDay);
        BigDecimal revenueThisMonth = bookingRepo.sumFinalFareByTenantAndStatusAndTripCompletedAtBetween(
                tenant, BookingStatus.COMPLETED, startOfMonth, endOfDay);
        if (revenueThisMonth == null) revenueThisMonth = BigDecimal.ZERO;

        long tripsToday = dailyTripRepo.countByTenantAndTripDate(tenant, today);
        long unassignedTrips = dailyTripRepo.countByTenantAndTripDateAndDriverIsNull(tenant, today);
        long activeSosAlerts = sosRepo.countByTenantAndStatus(tenant, SosStatus.ACTIVE);
        long pendingInvoices = invoiceRepo.countByTenantAndStatus(tenant, InvoiceStatus.DRAFT)
                + invoiceRepo.countByTenantAndStatus(tenant, InvoiceStatus.SENT);

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
