package com.carbooking.modules.dailyschedule.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.entity.DailyTripSkipDate;
import com.carbooking.modules.dailyschedule.application.DailyTripService;
import com.carbooking.modules.dailyschedule.dto.request.AssignDriverToDailyTripRequest;
import com.carbooking.modules.dailyschedule.dto.request.VerifyDailyTripOtpRequest;
import com.carbooking.modules.dailyschedule.dto.response.DailyTripResponse;
import com.carbooking.modules.dailyschedule.dto.response.MyTodayTripResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Daily Trips")
@RestController
@RequestMapping("/api/daily-trips")
@RequiredArgsConstructor
public class DailyTripController {

    private final DailyTripService dailyTripService;

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyTripResponse>>> listByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseHelper.ok(dailyTripService.listByDate(date));
    }

    @PreAuthorize("hasAnyRole('FLEET_MANAGER', 'DRIVER')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DailyTripResponse>> get(@PathVariable UUID id) {
        return ResponseHelper.ok(dailyTripService.get(id));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @PostMapping("/{id}/assign-driver")
    public ResponseEntity<ApiResponse<DailyTripResponse>> assignDriver(
            @PathVariable UUID id,
            @Valid @RequestBody AssignDriverToDailyTripRequest request) {
        return ResponseHelper.ok(dailyTripService.assignDriver(id, request));
    }

    @PreAuthorize("hasRole('FLEET_MANAGER')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelTrip(@PathVariable UUID id) {
        dailyTripService.cancelTrip(id);
        return ResponseHelper.noContent();
    }

    @PreAuthorize("hasRole('DRIVER')")
    @GetMapping("/my-today-driver")
    public ResponseEntity<ApiResponse<DailyTripResponse>> getMyTodayTripAsDriver() {
        return ResponseHelper.ok(dailyTripService.getMyTodayTrip());
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{id}/passengers/{pid}/board")
    public ResponseEntity<ApiResponse<DailyTripResponse>> boardPassenger(
            @PathVariable UUID id,
            @PathVariable UUID pid,
            @Valid @RequestBody VerifyDailyTripOtpRequest request) {
        return ResponseHelper.ok(dailyTripService.boardPassenger(id, pid, request));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{id}/passengers/{pid}/drop")
    public ResponseEntity<ApiResponse<DailyTripResponse>> dropPassenger(
            @PathVariable UUID id,
            @PathVariable UUID pid,
            @Valid @RequestBody VerifyDailyTripOtpRequest request) {
        return ResponseHelper.ok(dailyTripService.dropPassenger(id, pid, request));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{id}/passengers/{pid}/no-show")
    public ResponseEntity<ApiResponse<DailyTripResponse>> markNoShow(
            @PathVariable UUID id,
            @PathVariable UUID pid) {
        return ResponseHelper.ok(dailyTripService.markNoShow(id, pid));
    }

    @PreAuthorize("hasRole('DRIVER')")
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<DailyTripResponse>> completeTrip(@PathVariable UUID id) {
        return ResponseHelper.ok(dailyTripService.completeTrip(id));
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/my-today")
    public ResponseEntity<ApiResponse<MyTodayTripResponse>> getMyTodayAsEmployee() {
        return ResponseHelper.ok(dailyTripService.getMyTodayAsEmployee());
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/my-schedule")
    public ResponseEntity<ApiResponse<List<MyTodayTripResponse>>> getMySchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseHelper.ok(dailyTripService.getMySchedule(from, to));
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PostMapping("/enrollments/{pid}/skip")
    public ResponseEntity<Void> skipDate(
            @PathVariable UUID pid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        dailyTripService.skipDate(pid, date);
        return ResponseHelper.noContent();
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @DeleteMapping("/enrollments/{pid}/skip/{date}")
    public ResponseEntity<Void> undoSkip(
            @PathVariable UUID pid,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        dailyTripService.undoSkip(pid, date);
        return ResponseHelper.noContent();
    }

    @PreAuthorize("hasAnyRole('EMPLOYEE', 'FLEET_MANAGER', 'CORPORATE_ADMIN')")
    @GetMapping("/enrollments/{pid}/skips")
    public ResponseEntity<ApiResponse<List<DailyTripSkipDate>>> listSkipDates(
            @PathVariable UUID pid) {
        return ResponseHelper.ok(dailyTripService.listSkipDates(pid));
    }
}
