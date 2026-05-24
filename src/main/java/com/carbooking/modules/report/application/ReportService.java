package com.carbooking.modules.report.application;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Booking;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.report.dto.response.CorporateSpendResponse;
import com.carbooking.modules.report.dto.response.FleetSummaryResponse;
import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.client.domain.port.CorporateClientPort;
import com.carbooking.modules.fleet.domain.port.VehiclePort;
import com.carbooking.modules.report.dto.response.OverviewStatsResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService extends TenantSupport {

    private final BookingPort bookingRepository;
    private final CorporateClientPort corporateClientRepository;
    private final VehiclePort vehicleRepository;

    private static final Set<String> CANCELLED_STATUSES = Set.of(
            "CANCELLED_BY_EMPLOYEE", "CANCELLED_BY_ADMIN",
            "CANCELLED_BY_FLEET_MANAGER", "CANCELLED_BY_DRIVER");

    public ReportService(BookingPort bookingRepository,
                         CorporateClientPort corporateClientRepository,
                         VehiclePort vehicleRepository) {
        this.bookingRepository = bookingRepository;
        this.corporateClientRepository = corporateClientRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public FleetSummaryResponse fleetSummary(LocalDate fromDate, LocalDate toDate) {
        Tenant tenant = requireTenant();

        List<Booking> all = bookingRepository.findByTenant(tenant, Pageable.unpaged()).getContent();

        if (fromDate != null || toDate != null) {
            all = all.stream().filter(b -> {
                if (b.getCreatedAt() == null) return false;
                LocalDate created = b.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
                boolean afterFrom = fromDate == null || !created.isBefore(fromDate);
                boolean beforeTo = toDate == null || !created.isAfter(toDate);
                return afterFrom && beforeTo;
            }).collect(Collectors.toList());
        }

        List<Booking> completed = all.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED).collect(Collectors.toList());
        long cancelled = all.stream()
                .filter(b -> CANCELLED_STATUSES.contains(b.getStatus().name())).count();

        BigDecimal totalRevenue = completed.stream()
                .map(b -> b.getFinalFare() != null ? b.getFinalFare()
                        : (b.getEstimatedFare() != null ? b.getEstimatedFare() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgFare = completed.isEmpty() ? BigDecimal.ZERO
                : totalRevenue.divide(new BigDecimal(completed.size()), 2, RoundingMode.HALF_UP);

        Map<String, Long> driverTrips = completed.stream()
                .filter(b -> b.getDriver() != null)
                .collect(Collectors.groupingBy(b -> b.getDriver().getUser().getFullName(), Collectors.counting()));

        List<FleetSummaryResponse.DriverStat> topDrivers = driverTrips.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> FleetSummaryResponse.DriverStat.builder()
                        .driverName(e.getKey()).completedTrips(e.getValue()).build())
                .collect(Collectors.toList());

        Map<String, Long> vehicleTrips = completed.stream()
                .filter(b -> b.getVehicle() != null)
                .collect(Collectors.groupingBy(b -> b.getVehicle().getPlateNumber(), Collectors.counting()));

        Map<String, String> vehicleTypes = completed.stream()
                .filter(b -> b.getVehicle() != null)
                .collect(Collectors.toMap(b -> b.getVehicle().getPlateNumber(),
                        b -> b.getVehicle().getVehicleType().name(), (a, b) -> a));

        List<FleetSummaryResponse.VehicleUtilization> utilization = vehicleTrips.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> FleetSummaryResponse.VehicleUtilization.builder()
                        .plateNumber(e.getKey())
                        .vehicleType(vehicleTypes.getOrDefault(e.getKey(), ""))
                        .trips(e.getValue()).build())
                .collect(Collectors.toList());

        return FleetSummaryResponse.builder()
                .totalBookings(all.size())
                .completedBookings(completed.size())
                .cancelledBookings(cancelled)
                .totalRevenue(totalRevenue)
                .averageFare(avgFare)
                .topDrivers(topDrivers)
                .vehicleUtilization(utilization)
                .build();
    }

    @Transactional(readOnly = true)
    public CorporateSpendResponse corporateSpend(UUID corporateClientId, LocalDate fromDate, LocalDate toDate) {
        CorporateClient client = corporateClientRepository.findById(corporateClientId)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found"));
        assertSameTenant(client.getTenant().getId());

        List<Booking> completed = bookingRepository.findByCorporateClient(client, Pageable.unpaged())
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .collect(Collectors.toList());

        if (fromDate != null || toDate != null) {
            completed = completed.stream().filter(b -> {
                if (b.getTripCompletedAt() == null) return false;
                LocalDate date = b.getTripCompletedAt().atZone(ZoneOffset.UTC).toLocalDate();
                boolean afterFrom = fromDate == null || !date.isBefore(fromDate);
                boolean beforeTo = toDate == null || !date.isAfter(toDate);
                return afterFrom && beforeTo;
            }).collect(Collectors.toList());
        }

        BigDecimal totalSpend = completed.stream()
                .map(b -> b.getFinalFare() != null ? b.getFinalFare()
                        : (b.getEstimatedFare() != null ? b.getEstimatedFare() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Booking> finalCompleted = completed;
        List<CorporateSpendResponse.EmployeeStat> topEmployees = completed.stream()
                .collect(Collectors.groupingBy(b -> b.getEmployee().getFullName()))
                .entrySet().stream()
                .map(e -> {
                    BigDecimal spend = e.getValue().stream()
                            .map(b -> b.getFinalFare() != null ? b.getFinalFare()
                                    : (b.getEstimatedFare() != null ? b.getEstimatedFare() : BigDecimal.ZERO))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return CorporateSpendResponse.EmployeeStat.builder()
                            .employeeName(e.getKey()).trips(e.getValue().size()).totalSpend(spend).build();
                })
                .sorted(Comparator.comparing(CorporateSpendResponse.EmployeeStat::getTotalSpend).reversed())
                .limit(5)
                .collect(Collectors.toList());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        List<CorporateSpendResponse.MonthlyBreakdown> breakdown = completed.stream()
                .filter(b -> b.getTripCompletedAt() != null)
                .collect(Collectors.groupingBy(b ->
                        YearMonth.from(b.getTripCompletedAt().atZone(ZoneOffset.UTC)).format(fmt)))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    BigDecimal spend = e.getValue().stream()
                            .map(b -> b.getFinalFare() != null ? b.getFinalFare()
                                    : (b.getEstimatedFare() != null ? b.getEstimatedFare() : BigDecimal.ZERO))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return CorporateSpendResponse.MonthlyBreakdown.builder()
                            .month(e.getKey()).trips(e.getValue().size()).spend(spend).build();
                })
                .collect(Collectors.toList());

        return CorporateSpendResponse.builder()
                .totalBookings(completed.size())
                .totalSpend(totalSpend)
                .topEmployees(topEmployees)
                .monthlyBreakdown(breakdown)
                .build();
    }

    @Transactional(readOnly = true)
    public OverviewStatsResponse getOverviewStats() {
        Tenant tenant = requireTenant();

        java.time.Instant mtdStart = java.time.YearMonth.now(java.time.ZoneOffset.UTC)
                .atDay(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        java.time.Instant now = java.time.Instant.now();

        BigDecimal revenueMtd = bookingRepository.sumFinalFareByTenantAndStatusAndTripCompletedAtBetween(
                tenant, BookingStatus.COMPLETED, mtdStart, now);
        if (revenueMtd == null) revenueMtd = BigDecimal.ZERO;

        long bookingsMtd = bookingRepository.countByTenantAndCreatedAtBetween(tenant, mtdStart, now);
        long activeTrips = bookingRepository.countActiveByTenant(tenant);
        long pendingApprovals = bookingRepository.countByTenantAndStatus(tenant, BookingStatus.PENDING_APPROVAL);

        long totalVehicles = vehicleRepository.countByTenant(tenant);
        long availableCars = vehicleRepository.countByTenantAndStatus(tenant, VehicleStatus.ACTIVE);
        double occupancyPct = totalVehicles > 0 ? (activeTrips * 100.0 / totalVehicles) : 0.0;

        // Top vehicles by trip count
        List<Booking> allCompleted = bookingRepository.findByTenantAndStatus(tenant, BookingStatus.COMPLETED);
        List<OverviewStatsResponse.TopVehicleEntry> topVehicles = allCompleted.stream()
                .filter(b -> b.getVehicle() != null)
                .collect(Collectors.groupingBy(b -> b.getVehicle().getId(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<java.util.UUID, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Booking sample = allCompleted.stream()
                            .filter(b -> b.getVehicle() != null && b.getVehicle().getId().equals(e.getKey()))
                            .findFirst().orElseThrow();
                    String name = sample.getVehicle().getPlateNumber();
                    return OverviewStatsResponse.TopVehicleEntry.builder()
                            .vehicleName(name).tripCount(e.getValue()).build();
                })
                .collect(Collectors.toList());

        // Top corporate clients by revenue
        List<OverviewStatsResponse.TopClientEntry> topClients = allCompleted.stream()
                .filter(b -> b.getCorporateClient() != null && b.getFinalFare() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getCorporateClient().getId(),
                        Collectors.reducing(BigDecimal.ZERO, Booking::getFinalFare, BigDecimal::add)))
                .entrySet().stream()
                .sorted(Map.Entry.<java.util.UUID, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Booking sample = allCompleted.stream()
                            .filter(b -> b.getCorporateClient() != null && b.getCorporateClient().getId().equals(e.getKey()))
                            .findFirst().orElseThrow();
                    return OverviewStatsResponse.TopClientEntry.builder()
                            .clientName(sample.getCorporateClient().getCompanyName())
                            .revenue(e.getValue()).build();
                })
                .collect(Collectors.toList());

        return OverviewStatsResponse.builder()
                .revenueMtd(revenueMtd)
                .bookingsMtd(bookingsMtd)
                .activeTrips(activeTrips)
                .availableCars(availableCars)
                .pendingApprovals(pendingApprovals)
                .fleetOccupancyPct(occupancyPct)
                .topVehicles(topVehicles)
                .topCorporateClients(topClients)
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] exportReport(String type) {
        Tenant tenant = requireTenant();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);

            if ("bookings".equals(type)) {
                List<Booking> bookings = bookingRepository.findByTenant(tenant, Pageable.unpaged()).getContent();
                try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                        .setHeader("id", "employee", "vehicle", "scheduledAt", "status", "finalFare", "driver")
                        .build())) {
                    for (Booking b : bookings) {
                        printer.printRecord(
                                b.getId(),
                                b.getEmployee() != null ? b.getEmployee().getFullName() : "",
                                b.getVehicle() != null ? b.getVehicle().getPlateNumber() : "",
                                b.getScheduledAt(),
                                b.getStatus(),
                                b.getFinalFare() != null ? b.getFinalFare() : "",
                                b.getDriver() != null ? b.getDriver().getUser().getFullName() : ""
                        );
                    }
                }
            } else if ("revenue".equals(type)) {
                List<Booking> completed = bookingRepository.findByTenantAndStatus(tenant, BookingStatus.COMPLETED);
                Map<String, List<Booking>> byMonth = completed.stream()
                        .filter(b -> b.getTripCompletedAt() != null)
                        .collect(Collectors.groupingBy(b ->
                                b.getTripCompletedAt().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM"))));
                try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                        .setHeader("month", "totalRevenue", "bookingCount", "avgFare")
                        .build())) {
                    byMonth.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
                        BigDecimal total = e.getValue().stream()
                                .map(b -> b.getFinalFare() != null ? b.getFinalFare() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        int count = e.getValue().size();
                        BigDecimal avg = count > 0 ? total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                        try { printer.printRecord(e.getKey(), total, count, avg); }
                        catch (IOException ex) { throw new RuntimeException(ex); }
                    });
                }
            } else {
                throw new com.carbooking.common.exception.BusinessRuleException("Unknown export type: " + type + ". Use: bookings, revenue");
            }

            writer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV export", e);
        }
    }
}
