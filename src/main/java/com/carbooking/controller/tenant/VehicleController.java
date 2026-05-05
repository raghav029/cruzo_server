package com.carbooking.controller.tenant;

import com.carbooking.common.enums.VehicleStatus;
import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.dto.request.vehicle.CreateVehicleRequest;
import com.carbooking.dto.request.vehicle.UpdateVehicleRequest;
import com.carbooking.dto.response.vehicle.VehicleResponse;
import com.carbooking.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Vehicles")
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> create(
            @Valid @RequestBody CreateVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(vehicleService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<VehicleResponse>>> list(
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                new PagedResponse<>(vehicleService.list(status, PageRequest.of(page, size)))));
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<VehicleResponse>> get(@PathVariable UUID vehicleId) {
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.get(vehicleId)));
    }

    @PutMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<VehicleResponse>> update(
            @PathVariable UUID vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.update(vehicleId, request)));
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> delete(@PathVariable UUID vehicleId) {
        vehicleService.delete(vehicleId);
        return ResponseEntity.noContent().build();
    }
}
