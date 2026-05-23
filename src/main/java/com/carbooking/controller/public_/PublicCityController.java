package com.carbooking.controller.public_;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.dto.response.city.CityResponse;
import com.carbooking.service.CityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Public — Cities")
@RestController
@RequestMapping("/api/public/cities")
@RequiredArgsConstructor
public class PublicCityController {

    private final CityService cityService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CityResponse>>> listActive(
            @RequestParam UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(cityService.listActivePublic(tenantId)));
    }
}
