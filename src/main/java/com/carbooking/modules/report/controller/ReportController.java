package com.carbooking.modules.report.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.report.application.ReportService;
import com.carbooking.modules.report.dto.response.CorporateSpendResponse;
import com.carbooking.modules.report.dto.response.FleetSummaryResponse;
import com.carbooking.modules.report.dto.response.OverviewStatsResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "Reports")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FLEET_MANAGER', 'CORPORATE_ADMIN')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/fleet-summary")
    public ResponseEntity<ApiResponse<FleetSummaryResponse>> fleetSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseHelper.ok(reportService.fleetSummary(fromDate, toDate));
    }

    @GetMapping("/corporate-spend")
    public ResponseEntity<ApiResponse<CorporateSpendResponse>> corporateSpend(
            @RequestParam UUID corporateClientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseHelper.ok(reportService.corporateSpend(corporateClientId, fromDate, toDate));
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<OverviewStatsResponse>> overview() {
        return ResponseHelper.ok(reportService.getOverviewStats());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "bookings") String type) {
        byte[] data = reportService.exportReport(type);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + type + ".csv")
                .header("Content-Type", "text/csv")
                .body(data);
    }
}
