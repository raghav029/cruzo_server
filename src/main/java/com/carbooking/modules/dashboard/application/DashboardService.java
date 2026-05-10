package com.carbooking.modules.dashboard.application;

import com.carbooking.common.enums.*;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Driver;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.Vehicle;
import com.carbooking.modules.dashboard.dto.response.DashboardSummaryResponse;
import com.carbooking.modules.dashboard.dto.response.DashboardSummaryResponse.HourlyCount;
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
import java.util.*;

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
        String tz = tenant.getTimezone() != null ? tenant.getTimezone() : "Asia/Kolkata";
        ZoneId zone = ZoneId.of(tz);
        LocalDate today = LocalDate.now(zone);

        Instant startOfDay   = today.atStartOfDay(zone).toInstant();
        Instant endOfDay     = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();

        long totalVehicles    = vehicleRepo.countByTenant(tenant);
        long vehiclesInTrip   = vehicleRepo.countByTenantAndStatus(tenant, VehicleStatus.IN_TRIP);
        long totalDrivers     = driverRepo.countByTenant(tenant);
        long availableDrivers = driverRepo.countByTenantAndAvailability(tenant, DriverAvailability.AVAILABLE);
        long pendingApprovals = bookingRepo.countByTenantAndStatus(tenant, BookingStatus.PENDING_APPROVAL);
        long activeTrips      = bookingRepo.countByTenantAndStatus(tenant, BookingStatus.IN_PROGRESS);
        long totalBookingsThisMonth = bookingRepo.countByTenantAndCreatedAtBetween(tenant, startOfMonth, endOfDay);
        BigDecimal revenueThisMonth = bookingRepo.sumFinalFareByTenantAndStatusAndTripCompletedAtBetween(
                tenant, BookingStatus.COMPLETED, startOfMonth, endOfDay);
        if (revenueThisMonth == null) revenueThisMonth = BigDecimal.ZERO;

        long tripsToday      = dailyTripRepo.countByTenantAndTripDate(tenant, today);
        long unassignedTrips = dailyTripRepo.countByTenantAndTripDateAndDriverIsNull(tenant, today);
        long activeSosAlerts = sosRepo.countByTenantAndStatus(tenant, SosStatus.ACTIVE);
        long pendingInvoices = invoiceRepo.countByTenantAndStatus(tenant, InvoiceStatus.DRAFT)
                + invoiceRepo.countByTenantAndStatus(tenant, InvoiceStatus.SENT);

        LocalDate in30Days = today.plusDays(30);
        List<Driver>  expDrivers  = driverRepo.findByTenantAndLicenseExpiryBefore(tenant, in30Days);
        List<Vehicle> expVehicles = vehicleRepo.findByTenantAndInsuranceExpiryBeforeAndStatusNot(
                tenant, in30Days, VehicleStatus.INACTIVE);
        long expiringDocuments = expDrivers.size() + expVehicles.size();
        LocalDate sparkStart = today.minusDays(12);
        Instant sparkFromInstant = sparkStart.atStartOfDay(zone).toInstant();

        List<Integer> tripsSparkData = fillDailyIntegers(
                dailyTripRepo.countGroupedByDay(tenant.getId(), sparkStart, today), sparkStart, today);
        List<Integer> unassignedSparkData = fillDailyIntegers(
                dailyTripRepo.countUnassignedGroupedByDay(tenant.getId(), sparkStart, today), sparkStart, today);
        List<Integer> revenueSparkData = fillDailyRevenue(
                bookingRepo.sumRevenueGroupedByDay(tenant.getId(), tz, sparkFromInstant, endOfDay), sparkStart, today);

        Instant sevenDaysAgo  = today.minusDays(7).atStartOfDay(zone).toInstant();
        Instant thirtyDaysAgo = today.minusDays(30).atStartOfDay(zone).toInstant();

        List<HourlyCount> tripsByHourToday = buildHourlyData(
                bookingRepo.countGroupedByHour(tenant.getId(), tz, startOfDay, endOfDay), 1);
        List<HourlyCount> tripsByHour7d = buildHourlyData(
                bookingRepo.countGroupedByHour(tenant.getId(), tz, sevenDaysAgo, endOfDay), 7);
        List<HourlyCount> tripsByHour30d = buildHourlyData(
                bookingRepo.countGroupedByHour(tenant.getId(), tz, thirtyDaysAgo, endOfDay), 30);

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
                .tripsSparkData(tripsSparkData)
                .revenueSparkData(revenueSparkData)
                .unassignedSparkData(unassignedSparkData)
                .tripsByHourToday(tripsByHourToday)
                .tripsByHour7d(tripsByHour7d)
                .tripsByHour30d(tripsByHour30d)
                .build();
    }

    private List<Integer> fillDailyIntegers(List<Object[]> rows, LocalDate from, LocalDate to) {
        Map<LocalDate, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(((java.sql.Date) row[0]).toLocalDate(), ((Number) row[1]).intValue());
        }
        List<Integer> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            result.add(map.getOrDefault(d, 0));
        }
        return result;
    }

    private List<Integer> fillDailyRevenue(List<Object[]> rows, LocalDate from, LocalDate to) {
        Map<LocalDate, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(((java.sql.Date) row[0]).toLocalDate(), ((Number) row[1]).intValue());
        }
        List<Integer> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            result.add(map.getOrDefault(d, 0));
        }
        return result;
    }

    private static final int HOUR_START = 6;
    private static final int HOUR_END   = 21;
    private static final String[] HOUR_LABELS = {
        "6a","7","8","9","10","11","12","1p","2","3","4","5","6","7","8","9p"
    };

    private List<HourlyCount> buildHourlyData(List<Object[]> rows, int days) {
        Map<Integer, Long> totals = new HashMap<>();
        for (Object[] row : rows) {
            totals.merge(((Number) row[0]).intValue(), ((Number) row[1]).longValue(), Long::sum);
        }
        List<HourlyCount> result = new ArrayList<>();
        for (int h = HOUR_START; h <= HOUR_END; h++) {
            long total = totals.getOrDefault(h, 0L);
            int avg = days <= 1 ? (int) total : (int) Math.round((double) total / days);
            result.add(new HourlyCount(HOUR_LABELS[h - HOUR_START], avg));
        }
        return result;
    }
}
