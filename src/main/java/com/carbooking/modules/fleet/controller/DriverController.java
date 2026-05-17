package com.carbooking.modules.fleet.controller;

import com.carbooking.common.enums.DriverAvailability;
import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.fleet.application.DriverService;
import com.carbooking.modules.fleet.dto.request.CreateDriverRequest;
import com.carbooking.modules.fleet.dto.request.UpdateDriverRequest;
import com.carbooking.modules.fleet.dto.request.UpdateDriverSelfRequest;
import com.carbooking.modules.fleet.dto.response.DriverResponse;
import com.carbooking.modules.fleet.dto.response.DriverStatsResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "Drivers")
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FLEET_MANAGER')")
public class DriverController {

    private final DriverService driverService;

    @PostMapping
    public ResponseEntity<ApiResponse<DriverResponse>> create(
            @Valid @RequestBody CreateDriverRequest request) {
        return ResponseHelper.created(driverService.create(request));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER') or hasRole('DRIVER')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<DriverResponse>> getMe() {
        return ResponseHelper.ok(driverService.getMe());
    }

    @PreAuthorize("hasRole('DRIVER') or hasRole('FLEET_MANAGER')")
    @GetMapping("/me/stats")
    public ResponseEntity<ApiResponse<DriverStatsResponse>> getMyStats() {
        return ResponseHelper.ok(driverService.getMyStats());
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<DriverResponse>> updateMe(
            @Valid @RequestBody UpdateDriverSelfRequest request) {
        return ResponseHelper.ok(driverService.updateMe(request));
    }

    @PreAuthorize("hasRole('DRIVER') or hasRole('FLEET_MANAGER')")
    @PatchMapping("/me/availability")
    public ResponseEntity<ApiResponse<DriverResponse>> updateMyAvailability(
            @RequestParam DriverAvailability availability) {
        return ResponseHelper.ok(driverService.updateMyAvailability(availability));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<DriverResponse>>> list(
            @RequestParam(required = false) DriverAvailability availability,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseHelper.ok(new PagedResponse<>(driverService.list(availability, PageRequest.of(page, size))));
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<ApiResponse<DriverResponse>> get(@PathVariable UUID driverId) {
        return ResponseHelper.ok(driverService.get(driverId));
    }

    @PutMapping("/{driverId}")
    public ResponseEntity<ApiResponse<DriverResponse>> update(
            @PathVariable UUID driverId,
            @Valid @RequestBody UpdateDriverRequest request) {
        return ResponseHelper.ok(driverService.update(driverId, request));
    }

    @DeleteMapping("/{driverId}")
    public ResponseEntity<Void> delete(@PathVariable UUID driverId) {
        driverService.delete(driverId);
        return ResponseHelper.noContent();
    }
}
