package com.carbooking.modules.dashboard.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.dashboard.application.DashboardService;
import com.carbooking.modules.dashboard.dto.response.CorporateDashboardResponse;
import com.carbooking.modules.dashboard.dto.response.FleetDashboardResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Dashboard")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/fleet")
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    public ResponseEntity<ApiResponse<FleetDashboardResponse>> getFleetDashboard() {
        return ResponseHelper.ok(dashboardService.getFleetDashboard());
    }

    @GetMapping("/corporate")
    @PreAuthorize("hasRole('CORPORATE_ADMIN')")
    public ResponseEntity<ApiResponse<CorporateDashboardResponse>> getCorporateDashboard() {
        return ResponseHelper.ok(dashboardService.getCorporateDashboard());
    }
}
