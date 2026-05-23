package com.carbooking.modules.public_.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.dto.response.public_.PublicVehicleDetailResponse;
import com.carbooking.dto.response.public_.PublicVehicleResponse;
import com.carbooking.service.PublicVehicleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Public — Car Catalog")
@RestController
@RequestMapping("/api/public/vehicles")
@RequiredArgsConstructor
public class PublicVehicleController {

    private final PublicVehicleService publicVehicleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicVehicleResponse>>> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) UUID cityId) {
        return ResponseHelper.ok(publicVehicleService.listPublished(tenantId, cityId));
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<PublicVehicleDetailResponse>> detail(
            @PathVariable UUID vehicleId,
            @RequestParam UUID tenantId) {
        return ResponseHelper.ok(publicVehicleService.getDetail(vehicleId, tenantId));
    }
}
