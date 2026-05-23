package com.carbooking.controller.tenant;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.dto.request.city.CreateCityRequest;
import com.carbooking.dto.request.city.UpdateCityRequest;
import com.carbooking.dto.response.city.CityResponse;
import com.carbooking.service.CityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Cities")
@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @PostMapping
    public ResponseEntity<ApiResponse<CityResponse>> create(
            @Valid @RequestBody CreateCityRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(cityService.create(req)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CityResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(cityService.listAll()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CityResponse>> update(
            @PathVariable UUID id,
            @RequestBody UpdateCityRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(cityService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        cityService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
