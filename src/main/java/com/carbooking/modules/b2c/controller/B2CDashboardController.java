package com.carbooking.modules.b2c.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.dto.response.b2c.B2CDashboardResponse;
import com.carbooking.service.B2CDashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "B2C — Dashboard")
@RestController
@RequestMapping("/api/b2c/dashboard")
@RequiredArgsConstructor
public class B2CDashboardController {

    private final B2CDashboardService b2cDashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<B2CDashboardResponse>> getDashboard() {
        return ResponseHelper.ok(b2cDashboardService.getDashboard());
    }
}
