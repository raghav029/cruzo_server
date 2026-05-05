package com.carbooking.modules.dailyschedule.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.dailyschedule.application.DailyScheduleService;
import com.carbooking.modules.dailyschedule.dto.request.*;
import com.carbooking.modules.dailyschedule.dto.response.DailySchedulePassengerResponse;
import com.carbooking.modules.dailyschedule.dto.response.DailyScheduleResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Daily Schedules")
@RestController
@RequestMapping("/api/daily-schedules")
@RequiredArgsConstructor
public class DailyScheduleController {

    private final DailyScheduleService dailyScheduleService;

    @PreAuthorize("hasRole('CORPORATE_ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<DailyScheduleResponse>> create(
            @Valid @RequestBody CreateDailyScheduleRequest request) {
        return ResponseHelper.created(dailyScheduleService.create(request));
    }

    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'CORPORATE_ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<DailyScheduleResponse>>> list(Pageable pageable) {
        return ResponseHelper.ok(new PagedResponse<>(dailyScheduleService.list(pageable)));
    }

    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'CORPORATE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DailyScheduleResponse>> get(@PathVariable UUID id) {
        return ResponseHelper.ok(dailyScheduleService.get(id));
    }

    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'CORPORATE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DailyScheduleResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDailyScheduleRequest request) {
        return ResponseHelper.ok(dailyScheduleService.update(id, request));
    }

    @PreAuthorize("hasRole('CORPORATE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        dailyScheduleService.deactivate(id);
        return ResponseHelper.noContent();
    }

    @PreAuthorize("hasRole('CORPORATE_ADMIN')")
    @PostMapping("/{id}/passengers")
    public ResponseEntity<ApiResponse<DailySchedulePassengerResponse>> enrollPassenger(
            @PathVariable UUID id,
            @Valid @RequestBody EnrollPassengerRequest request) {
        return ResponseHelper.created(dailyScheduleService.enrollPassenger(id, request));
    }

    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'CORPORATE_ADMIN')")
    @GetMapping("/{id}/passengers")
    public ResponseEntity<ApiResponse<List<DailySchedulePassengerResponse>>> listPassengers(
            @PathVariable UUID id) {
        return ResponseHelper.ok(dailyScheduleService.listPassengers(id));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @GetMapping("/{id}/passengers/unsequenced")
    public ResponseEntity<ApiResponse<List<DailySchedulePassengerResponse>>> listUnsequencedPassengers(
            @PathVariable UUID id) {
        return ResponseHelper.ok(dailyScheduleService.listUnsequencedPassengers(id));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @PutMapping("/{id}/passengers/{pid}/sequence")
    public ResponseEntity<ApiResponse<DailySchedulePassengerResponse>> assignStopSequence(
            @PathVariable UUID id,
            @PathVariable UUID pid,
            @Valid @RequestBody AssignStopSequenceRequest request) {
        return ResponseHelper.ok(dailyScheduleService.assignStopSequence(id, pid, request));
    }

    @PreAuthorize("hasRole('CORPORATE_ADMIN')")
    @DeleteMapping("/{id}/passengers/{pid}")
    public ResponseEntity<Void> removePassenger(
            @PathVariable UUID id,
            @PathVariable UUID pid) {
        dailyScheduleService.removePassenger(id, pid);
        return ResponseHelper.noContent();
    }
}
