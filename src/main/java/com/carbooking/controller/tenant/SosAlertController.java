package com.carbooking.controller.tenant;

import com.carbooking.common.enums.SosStatus;
import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.dto.request.sos.ResolveSosRequest;
import com.carbooking.dto.request.sos.TriggerSosRequest;
import com.carbooking.dto.response.sos.SosAlertResponse;
import com.carbooking.service.SosAlertService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sos")
@RequiredArgsConstructor
@Tag(name = "SOS Alerts")
public class SosAlertController {

    private final SosAlertService sosAlertService;

    @PostMapping
    public ResponseEntity<ApiResponse<SosAlertResponse>> trigger(@RequestBody TriggerSosRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(sosAlertService.trigger(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SosAlertResponse>>> list(
            @RequestParam(required = false) SosStatus status,
            Pageable pageable) {
        Page<SosAlertResponse> page = sosAlertService.list(status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(new PagedResponse<>(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SosAlertResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(sosAlertService.get(id)));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<SosAlertResponse>> resolve(
            @PathVariable UUID id,
            @RequestBody ResolveSosRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(sosAlertService.resolve(id, request)));
    }
}
