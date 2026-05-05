package com.carbooking.controller.tenant;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.dto.request.dailyschedule.*;
import com.carbooking.dto.response.dailyschedule.DailySchedulePassengerResponse;
import com.carbooking.dto.response.dailyschedule.DailyScheduleResponse;
import com.carbooking.service.DailyScheduleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Daily Schedules")
@RestController
@RequestMapping("/api/daily-schedules")
@RequiredArgsConstructor
public class DailyScheduleController {

    private final DailyScheduleService dailyScheduleService;

    @PostMapping
    public ResponseEntity<ApiResponse<DailyScheduleResponse>> create(
            @Valid @RequestBody CreateDailyScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(dailyScheduleService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<DailyScheduleResponse>>> list(Pageable pageable) {
        var page = dailyScheduleService.list(pageable);
        return ResponseEntity.ok(ApiResponse.ok(new PagedResponse<>(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DailyScheduleResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(dailyScheduleService.get(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DailyScheduleResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDailyScheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dailyScheduleService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        dailyScheduleService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/passengers")
    public ResponseEntity<ApiResponse<DailySchedulePassengerResponse>> enrollPassenger(
            @PathVariable UUID id,
            @Valid @RequestBody EnrollPassengerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(dailyScheduleService.enrollPassenger(id, request)));
    }

    @GetMapping("/{id}/passengers")
    public ResponseEntity<ApiResponse<List<DailySchedulePassengerResponse>>> listPassengers(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(dailyScheduleService.listPassengers(id)));
    }

    @GetMapping("/{id}/passengers/unsequenced")
    public ResponseEntity<ApiResponse<List<DailySchedulePassengerResponse>>> listUnsequencedPassengers(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(dailyScheduleService.listUnsequencedPassengers(id)));
    }

    @PutMapping("/{id}/passengers/{pid}/sequence")
    public ResponseEntity<ApiResponse<DailySchedulePassengerResponse>> assignStopSequence(
            @PathVariable UUID id,
            @PathVariable UUID pid,
            @Valid @RequestBody AssignStopSequenceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dailyScheduleService.assignStopSequence(id, pid, request)));
    }

    @DeleteMapping("/{id}/passengers/{pid}")
    public ResponseEntity<Void> removePassenger(
            @PathVariable UUID id,
            @PathVariable UUID pid) {
        dailyScheduleService.removePassenger(id, pid);
        return ResponseEntity.noContent().build();
    }
}
