package com.carbooking.modules.client.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.client.application.CorporateEmployeeService;
import com.carbooking.modules.client.dto.request.UpdateEmployeeSelfRequest;
import com.carbooking.modules.client.dto.response.CorporateEmployeeResponse;
import com.carbooking.modules.client.dto.response.TravelPolicyResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Employee Self")
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeSelfController {

    private final CorporateEmployeeService employeeService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CORPORATE_ADMIN')")
    public ResponseEntity<ApiResponse<CorporateEmployeeResponse>> getMe() {
        return ResponseHelper.ok(employeeService.getMe());
    }

    @PatchMapping("/me")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CORPORATE_ADMIN')")
    public ResponseEntity<ApiResponse<CorporateEmployeeResponse>> updateMe(
            @Valid @RequestBody UpdateEmployeeSelfRequest request) {
        return ResponseHelper.ok(employeeService.updateMe(request));
    }

    @GetMapping("/me/policy")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'CORPORATE_ADMIN')")
    public ResponseEntity<ApiResponse<TravelPolicyResponse>> getMyPolicy() {
        return ResponseHelper.ok(employeeService.getMyPolicy());
    }
}
