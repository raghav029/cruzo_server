package com.carbooking.controller.tenant;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.dto.request.dailyschedule.AssignDriverToDailyTripRequest;
import com.carbooking.dto.request.dailyschedule.VerifyDailyTripOtpRequest;
import com.carbooking.dto.response.dailyschedule.DailyTripResponse;
import com.carbooking.dto.response.dailyschedule.MyTodayTripResponse;
import com.carbooking.entity.DailyTripSkipDate;
import com.carbooking.service.DailyTripService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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

    // FLEET_MANAGER endpoints

    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyTripResponse>>> listByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(dailyTripService.listByDate(date)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DailyTripResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(dailyTripService.get(id)));
    }

    @PostMapping("/{id}/assign-driver")
    public ResponseEntity<ApiResponse<DailyTripResponse>> assignDriver(
            @PathVariable UUID id,
            @Valid @RequestBody AssignDriverToDailyTripRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dailyTripService.assignDriver(id, request)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelTrip(@PathVariable UUID id) {
        dailyTripService.cancelTrip(id);
        return ResponseEntity.noContent().build();
    }

    // DRIVER endpoints

    @GetMapping("/my-today-driver")
    public ResponseEntity<ApiResponse<DailyTripResponse>> getMyTodayTripAsDriver() {
        return ResponseEntity.ok(ApiResponse.ok(dailyTripService.getMyTodayTrip()));
    }

    @PostMapping("/{id}/passengers/{pid}/board")
    public ResponseEntity<ApiResponse<DailyTripResponse>> boardPassenger(
            @PathVariable UUID id,
            @PathVariable UUID pid,
            @Valid @RequestBody VerifyDailyTripOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dailyTripService.boardPassenger(id, pid, request)));
    }

    @PostMapping("/{id}/passengers/{pid}/drop")
    public ResponseEntity<ApiResponse<DailyTripResponse>> dropPassenger(
            @PathVariable UUID id,
            @PathVariable UUID pid,
            @Valid @RequestBody VerifyDailyTripOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dailyTripService.dropPassenger(id, pid, request)));
    }

    @PostMapping("/{id}/passengers/{pid}/no-show")
    public ResponseEntity<ApiResponse<DailyTripResponse>> markNoShow(
            @PathVariable UUID id,
            @PathVariable UUID pid) {
        return ResponseEntity.ok(ApiResponse.ok(dailyTripService.markNoShow(id, pid)));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<DailyTripResponse>> completeTrip(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(dailyTripService.completeTrip(id)));
    }

    // EMPLOYEE endpoints

    @GetMapping("/my-today")
    public ResponseEntity<ApiResponse<MyTodayTripResponse>> getMyTodayAsEmployee() {
        return ResponseEntity.ok(ApiResponse.ok(dailyTripService.getMyTodayAsEmployee()));
    }

    @GetMapping("/my-schedule")
    public ResponseEntity<ApiResponse<List<MyTodayTripResponse>>> getMySchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(dailyTripService.getMySchedule(from, to)));
    }

    @PostMapping("/enrollments/{pid}/skip")
    public ResponseEntity<Void> skipDate(
            @PathVariable UUID pid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        dailyTripService.skipDate(pid, date);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/enrollments/{pid}/skip/{date}")
    public ResponseEntity<Void> undoSkip(
            @PathVariable UUID pid,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        dailyTripService.undoSkip(pid, date);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/enrollments/{pid}/skips")
    public ResponseEntity<ApiResponse<List<DailyTripSkipDate>>> listSkipDates(
            @PathVariable UUID pid) {
        return ResponseEntity.ok(ApiResponse.ok(dailyTripService.listSkipDates(pid)));
    }
}
