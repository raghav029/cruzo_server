package com.carbooking.modules.fleet.controller;

import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.fleet.application.VehicleService;
import com.carbooking.modules.fleet.dto.request.CreateVehicleRequest;
import com.carbooking.modules.fleet.dto.request.UpdateVehicleRequest;
import com.carbooking.modules.fleet.dto.response.VehicleResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "Vehicles")
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FLEET_MANAGER')")
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> create(
            @Valid @RequestBody CreateVehicleRequest request) {
        return ResponseHelper.created(vehicleService.create(request));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<VehicleResponse>>> list(
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseHelper.ok(new PagedResponse<>(vehicleService.list(status, PageRequest.of(page, size))));
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<VehicleResponse>> get(@PathVariable UUID vehicleId) {
        return ResponseHelper.ok(vehicleService.get(vehicleId));
    }

    @PutMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<VehicleResponse>> update(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request) {
        return ResponseHelper.ok(vehicleService.update(vehicleId, request));
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> delete(@PathVariable UUID vehicleId) {
        vehicleService.delete(vehicleId);
        return ResponseHelper.noContent();
    }
}
