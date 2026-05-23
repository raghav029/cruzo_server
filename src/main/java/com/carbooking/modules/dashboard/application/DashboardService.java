package com.carbooking.modules.dashboard.application;

import com.carbooking.common.enums.*;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.enums.BookingMode;
import com.carbooking.entity.enums.BookingType;
import com.carbooking.modules.dashboard.dto.response.CorporateDashboardResponse;
import com.carbooking.modules.dashboard.dto.response.CorporateDashboardResponse.UpcomingBookingItem;
import com.carbooking.modules.dashboard.dto.response.FleetDashboardResponse;
import com.carbooking.modules.dashboard.dto.response.FleetDashboardResponse.HourlyCount;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.dailyschedule.domain.port.DailyTripPort;
import com.carbooking.modules.fleet.domain.port.DriverPort;
import com.carbooking.modules.fleet.domain.port.VehiclePort;
import com.carbooking.modules.invoice.domain.port.InvoicePort;
import com.carbooking.modules.sos.domain.port.SosAlertPort;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.User;
import com.carbooking.entity.Booking;
import com.carbooking.repository.CustomerRepository;
import com.carbooking.repository.CorporateEmployeeRepository;
import com.carbooking.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService extends TenantSupport {

    private final BookingPort bookingRepo;
    private final VehiclePort vehicleRepo;
    private final DriverPort driverRepo;
    private final InvoicePort invoiceRepo;
    private final SosAlertPort sosRepo;
    private final DailyTripPort dailyTripRepo;
    private final CustomerRepository customerRepo;
    private final CorporateEmployeeRepository corpEmployeeRepo;
    private final UserRepository userRepo;

    public DashboardService(BookingPort bookingRepo, VehiclePort vehicleRepo,
                            DriverPort driverRepo, InvoicePort invoiceRepo,
                            SosAlertPort sosRepo, DailyTripPort dailyTripRepo,
                            CustomerRepository customerRepo,
                            CorporateEmployeeRepository corpEmployeeRepo,
                            UserRepository userRepo) {
        this.bookingRepo = bookingRepo;
        this.vehicleRepo = vehicleRepo;
        this.driverRepo = driverRepo;
        this.invoiceRepo = invoiceRepo;
        this.sosRepo = sosRepo;
        this.dailyTripRepo = dailyTripRepo;
        this.customerRepo = customerRepo;
        this.corpEmployeeRepo = corpEmployeeRepo;
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    public FleetDashboardResponse getFleetDashboard() {
        Tenant tenant = requireTenant();
        ZoneId zone = ZoneId.of(tenant.getTimezone() != null ? tenant.getTimezone() : "Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);

        Instant startOfDay       = today.atStartOfDay(zone).toInstant();
        Instant endOfDay         = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant startOfMonth     = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Instant startOfLastMonth = today.withDayOfMonth(1).minusMonths(1).atStartOfDay(zone).toInstant();
        Instant endOfLastMonth   = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();

        BigDecimal revenueThisMonth = nvl(bookingRepo.sumFinalFareByTenantAndStatusAndTripCompletedAtBetween(
                tenant, BookingStatus.COMPLETED, startOfMonth, endOfDay));
        BigDecimal revenueLastMonth = nvl(bookingRepo.sumFinalFareByTenantAndStatusAndTripCompletedAtBetween(
                tenant, BookingStatus.COMPLETED, startOfLastMonth, endOfLastMonth));

        Instant sparkFrom = today.minusDays(12).atStartOfDay(zone).toInstant();
        List<Integer> revenueSparkData = fillDailyRevenue(
                bookingRepo.sumRevenueGroupedByDay(tenant.getId(), zone.getId(), sparkFrom, endOfDay),
                today.minusDays(12), today);

        long totalBookingsThisMonth = bookingRepo.countByTenantAndCreatedAtBetween(tenant, startOfMonth, endOfDay);
        boolean hasB2C = tenant.getBookingMode() != BookingMode.CORPORATE;
        long b2cBookingsThisMonth = hasB2C
                ? bookingRepo.countByTenantAndBookingTypeAndCreatedAtBetween(tenant, BookingType.B2C, startOfMonth, endOfDay)
                : 0L;
        long corporateBookingsThisMonth = bookingRepo.countByTenantAndBookingTypeAndCreatedAtBetween(
                tenant, BookingType.CORPORATE, startOfMonth, endOfDay);
        long pendingApprovals = bookingRepo.countByTenantAndStatus(tenant, BookingStatus.PENDING_APPROVAL);
        long activeTrips      = bookingRepo.countByTenantAndStatus(tenant, BookingStatus.IN_PROGRESS);

        long totalVehicles  = vehicleRepo.countByTenant(tenant);
        long vehiclesInTrip = vehicleRepo.countByTenantAndStatus(tenant, VehicleStatus.IN_TRIP);
        long vehiclesIdle   = vehicleRepo.countByTenantAndStatus(tenant, VehicleStatus.ACTIVE);
        double fleetOccupancy = totalVehicles == 0 ? 0.0
                : Math.round((double) vehiclesInTrip / totalVehicles * 1000.0) / 10.0;

        long totalDrivers     = driverRepo.countByTenant(tenant);
        long availableDrivers = driverRepo.countByTenantAndAvailability(tenant, DriverAvailability.AVAILABLE);

        long tripsToday           = dailyTripRepo.countByTenantAndTripDate(tenant, today);
        long unassignedTripsToday = dailyTripRepo.countByTenantAndTripDateAndDriverIsNull(tenant, today);

        long activeSosAlerts = sosRepo.countByTenantAndStatus(tenant, SosStatus.ACTIVE);
        long pendingInvoices = invoiceRepo.countByTenantAndStatus(tenant, InvoiceStatus.DRAFT)
                + invoiceRepo.countByTenantAndStatus(tenant, InvoiceStatus.SENT);
        LocalDate in30Days = today.plusDays(30);
        long expiringDocuments = driverRepo.findByTenantAndLicenseExpiryBefore(tenant, in30Days).size()
                + vehicleRepo.findByTenantAndInsuranceExpiryBeforeAndStatusNot(tenant, in30Days, VehicleStatus.INACTIVE).size();

        long totalCustomers        = hasB2C ? customerRepo.countByTenantId(tenant.getId()) : 0L;
        long newCustomersThisMonth = hasB2C ? customerRepo.countByTenantIdAndCreatedAtBetween(
                tenant.getId(), startOfMonth, endOfDay) : 0L;

        Instant sevenDaysAgo  = today.minusDays(7).atStartOfDay(zone).toInstant();
        Instant thirtyDaysAgo = today.minusDays(30).atStartOfDay(zone).toInstant();
        List<HourlyCount> tripsByHourToday = buildHourlyData(
                bookingRepo.countGroupedByHour(tenant.getId(), zone.getId(), startOfDay, endOfDay), 1);
        List<HourlyCount> tripsByHour7d = buildHourlyData(
                bookingRepo.countGroupedByHour(tenant.getId(), zone.getId(), sevenDaysAgo, endOfDay), 7);
        List<HourlyCount> tripsByHour30d = buildHourlyData(
                bookingRepo.countGroupedByHour(tenant.getId(), zone.getId(), thirtyDaysAgo, endOfDay), 30);

        return FleetDashboardResponse.builder()
                .revenueThisMonth(revenueThisMonth)
                .revenueLastMonth(revenueLastMonth)
                .revenueSparkData(revenueSparkData)
                .totalBookingsThisMonth(totalBookingsThisMonth)
                .b2cBookingsThisMonth(b2cBookingsThisMonth)
                .corporateBookingsThisMonth(corporateBookingsThisMonth)
                .pendingApprovals(pendingApprovals)
                .activeTrips(activeTrips)
                .totalVehicles(totalVehicles)
                .vehiclesInTrip(vehiclesInTrip)
                .vehiclesIdle(vehiclesIdle)
                .fleetOccupancy(fleetOccupancy)
                .totalDrivers(totalDrivers)
                .availableDrivers(availableDrivers)
                .tripsToday(tripsToday)
                .unassignedTripsToday(unassignedTripsToday)
                .activeSosAlerts(activeSosAlerts)
                .pendingInvoices(pendingInvoices)
                .expiringDocuments(expiringDocuments)
                .totalCustomers(totalCustomers)
                .newCustomersThisMonth(newCustomersThisMonth)
                .tripsByHourToday(tripsByHourToday)
                .tripsByHour7d(tripsByHour7d)
                .tripsByHour30d(tripsByHour30d)
                .build();
    }

    @Transactional(readOnly = true)
    public CorporateDashboardResponse getCorporateDashboard() {
        Tenant tenant = requireTenant();
        ZoneId zone = ZoneId.of(tenant.getTimezone() != null ? tenant.getTimezone() : "Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);

        Instant startOfDay       = today.atStartOfDay(zone).toInstant();
        Instant endOfDay         = today.plusDays(1).atStartOfDay(zone).toInstant();
        Instant startOfMonth     = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Instant startOfLastMonth = today.withDayOfMonth(1).minusMonths(1).atStartOfDay(zone).toInstant();
        Instant endOfLastMonth   = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();

        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        if (!user.getTenant().getId().equals(currentTenantId())) {
            throw new UnauthorizedException("Access denied");
        }
        CorporateClient client = user.getCorporateClient();
        if (client == null) {
            throw new UnauthorizedException("No corporate client linked to this account");
        }

        long pendingApprovals       = bookingRepo.countByCorporateClientAndStatus(client, BookingStatus.PENDING_APPROVAL);
        long activeBookings         = bookingRepo.countByCorporateClientAndStatus(client, BookingStatus.IN_PROGRESS);
        long totalBookingsThisMonth = bookingRepo.countByCorporateClientAndCreatedAtBetween(client, startOfMonth, endOfDay);
        long cancelledThisMonth     = bookingRepo.countCancelledByCorporateClientAndCreatedAtBetween(client, startOfMonth, endOfDay);

        BigDecimal spendThisMonth = nvl(bookingRepo.sumSpendByCorporateClientAndTripCompletedAtBetween(
                client, startOfMonth, endOfDay));
        BigDecimal spendLastMonth = nvl(bookingRepo.sumSpendByCorporateClientAndTripCompletedAtBetween(
                client, startOfLastMonth, endOfLastMonth));
        List<Integer> spendSparkData = fillDailyRevenue(
                bookingRepo.sumSpendGroupedByDayForClient(
                        client.getId(), zone.getId(),
                        today.minusDays(12).atStartOfDay(zone).toInstant(), endOfDay),
                today.minusDays(12), today);

        long totalEmployees          = corpEmployeeRepo.countByCorporateClient(client);
        long employeesWithActiveTrip = bookingRepo.countByCorporateClientAndStatus(client, BookingStatus.IN_PROGRESS);

        List<Booking> upcomingRaw = bookingRepo.findUpcomingByCorporateClient(
                client, Instant.now(), PageRequest.of(0, 5));
        List<UpcomingBookingItem> upcomingBookings = upcomingRaw.stream()
                .map(b -> new UpcomingBookingItem(
                        b.getId(),
                        b.getEmployee() != null ? b.getEmployee().getFullName() : "Unknown",
                        b.getScheduledAt(),
                        b.getPickupAddress(),
                        b.getStatus().name()))
                .collect(Collectors.toList());

        return CorporateDashboardResponse.builder()
                .pendingApprovals(pendingApprovals)
                .activeBookings(activeBookings)
                .totalBookingsThisMonth(totalBookingsThisMonth)
                .cancelledThisMonth(cancelledThisMonth)
                .spendThisMonth(spendThisMonth)
                .spendLastMonth(spendLastMonth)
                .spendSparkData(spendSparkData)
                .totalEmployees(totalEmployees)
                .employeesWithActiveTrip(employeesWithActiveTrip)
                .tripsToday(0L)
                .passengersToday(0L)
                .upcomingBookings(upcomingBookings)
                .build();
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
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
