package com.carbooking.service;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.response.report.CorporateSpendResponse;
import com.carbooking.dto.response.report.FleetSummaryResponse;
import com.carbooking.entity.*;
import com.carbooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final BookingRepository bookingRepository;
    private final TenantRepository tenantRepository;
    private final CorporateClientRepository corporateClientRepository;
    private final CorporateEmployeeRepository employeeRepository;

    private static final Set<String> CANCELLED_STATUSES = Set.of(
            "CANCELLED_BY_EMPLOYEE", "CANCELLED_BY_ADMIN",
            "CANCELLED_BY_FLEET_MANAGER", "CANCELLED_BY_DRIVER");

    @Transactional(readOnly = true)
    public FleetSummaryResponse fleetSummary(LocalDate fromDate, LocalDate toDate) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        List<Booking> all = bookingRepository.findByTenant(tenant, Pageable.unpaged()).getContent();

        // Filter by date range if provided
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

        // Top 5 drivers by completed trips
        Map<String, Long> driverTrips = completed.stream()
                .filter(b -> b.getDriver() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getDriver().getUser().getFullName(), Collectors.counting()));

        List<FleetSummaryResponse.DriverStat> topDrivers = driverTrips.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> FleetSummaryResponse.DriverStat.builder()
                        .driverName(e.getKey()).completedTrips(e.getValue()).build())
                .collect(Collectors.toList());

        // Vehicle utilization
        Map<String, Long> vehicleTrips = completed.stream()
                .filter(b -> b.getVehicle() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getVehicle().getPlateNumber(), Collectors.counting()));

        Map<String, String> vehicleTypes = completed.stream()
                .filter(b -> b.getVehicle() != null)
                .collect(Collectors.toMap(
                        b -> b.getVehicle().getPlateNumber(),
                        b -> b.getVehicle().getVehicleType().name(),
                        (a, b) -> a));

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
        UUID tenantId = SecurityUtils.getCurrentTenantId();

        CorporateClient client = corporateClientRepository.findById(corporateClientId)
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found"));
        if (!client.getTenant().getId().equals(tenantId)) throw new UnauthorizedException("Access denied");

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

        // Top 5 employees
        Map<String, List<Booking>> byEmployee = completed.stream()
                .collect(Collectors.groupingBy(b -> b.getEmployee().getFullName()));

        List<CorporateSpendResponse.EmployeeStat> topEmployees = byEmployee.entrySet().stream()
                .map(e -> {
                    BigDecimal spend = e.getValue().stream()
                            .map(b -> b.getFinalFare() != null ? b.getFinalFare()
                                    : (b.getEstimatedFare() != null ? b.getEstimatedFare() : BigDecimal.ZERO))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return CorporateSpendResponse.EmployeeStat.builder()
                            .employeeName(e.getKey())
                            .trips(e.getValue().size())
                            .totalSpend(spend)
                            .build();
                })
                .sorted(Comparator.comparing(CorporateSpendResponse.EmployeeStat::getTotalSpend).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // Monthly breakdown
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, List<Booking>> byMonth = completed.stream()
                .filter(b -> b.getTripCompletedAt() != null)
                .collect(Collectors.groupingBy(b ->
                        YearMonth.from(b.getTripCompletedAt().atZone(ZoneOffset.UTC)).format(fmt)));

        List<CorporateSpendResponse.MonthlyBreakdown> breakdown = byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    BigDecimal spend = e.getValue().stream()
                            .map(b -> b.getFinalFare() != null ? b.getFinalFare()
                                    : (b.getEstimatedFare() != null ? b.getEstimatedFare() : BigDecimal.ZERO))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return CorporateSpendResponse.MonthlyBreakdown.builder()
                            .month(e.getKey())
                            .trips(e.getValue().size())
                            .spend(spend)
                            .build();
                })
                .collect(Collectors.toList());

        return CorporateSpendResponse.builder()
                .totalBookings(completed.size())
                .totalSpend(totalSpend)
                .topEmployees(topEmployees)
                .monthlyBreakdown(breakdown)
                .build();
    }
}
