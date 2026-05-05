package com.carbooking.controller.tenant;

import com.carbooking.common.enums.DriverAvailability;
import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.dto.request.driver.CreateDriverRequest;
import com.carbooking.dto.request.driver.UpdateDriverRequest;
import com.carbooking.dto.response.driver.DriverResponse;
import com.carbooking.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Drivers")
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PostMapping
    public ResponseEntity<ApiResponse<DriverResponse>> create(
            @Valid @RequestBody CreateDriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(driverService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<DriverResponse>>> list(
            @RequestParam(required = false) DriverAvailability availability,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                new PagedResponse<>(driverService.list(availability, PageRequest.of(page, size)))));
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<ApiResponse<DriverResponse>> get(@PathVariable UUID driverId) {
        return ResponseEntity.ok(ApiResponse.ok(driverService.get(driverId)));
    }

    @PutMapping("/{driverId}")
    public ResponseEntity<ApiResponse<DriverResponse>> update(
            @PathVariable UUID driverId,
            @Valid @RequestBody UpdateDriverRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(driverService.update(driverId, request)));
    }

    @DeleteMapping("/{driverId}")
    public ResponseEntity<Void> delete(@PathVariable UUID driverId) {
        driverService.delete(driverId);
        return ResponseEntity.noContent().build();
    }
}
