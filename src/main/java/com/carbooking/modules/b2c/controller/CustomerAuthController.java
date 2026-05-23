package com.carbooking.modules.b2c.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.request.b2c.SendOtpRequest;
import com.carbooking.dto.request.b2c.UpdateProfileRequest;
import com.carbooking.dto.request.b2c.VerifyOtpRequest;
import com.carbooking.dto.response.b2c.B2CAuthResponse;
import com.carbooking.dto.response.b2c.CustomerResponse;
import com.carbooking.dto.response.b2c.OtpResponse;
import com.carbooking.service.CustomerAuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "B2C — Customer Auth")
@RestController
@RequestMapping("/api/b2c/auth")
@RequiredArgsConstructor
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> sendOtp(
            @Valid @RequestBody SendOtpRequest req) {
        return ResponseHelper.ok(customerAuthService.sendOtp(req));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<B2CAuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest req) {
        return ResponseHelper.ok(customerAuthService.verifyOtp(req));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomerResponse>> me() {
        return ResponseHelper.ok(
            customerAuthService.getProfile(SecurityUtils.getCurrentCustomerId()));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest req) {
        return ResponseHelper.ok(
            customerAuthService.updateProfile(SecurityUtils.getCurrentCustomerId(), req));
    }
}
