package com.carbooking.modules.dashboard.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.dashboard.application.DashboardService;
import com.carbooking.modules.dashboard.dto.response.DashboardSummaryResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "Dashboard")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FLEET_MANAGER', 'CORPORATE_ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        return ResponseHelper.ok(dashboardService.getSummary());
    }
}
