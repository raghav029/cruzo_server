package com.carbooking.modules.document.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.document.application.DocumentExpiryService;
import com.carbooking.modules.document.dto.response.ExpiringDocumentsResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@Tag(name = "Document Expiry")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FLEET_MANAGER')")
public class DocumentExpiryController {

    private final DocumentExpiryService documentExpiryService;

    @GetMapping("/expiring")
    public ResponseEntity<ApiResponse<ExpiringDocumentsResponse>> getExpiringDocuments() {
        return ResponseHelper.ok(documentExpiryService.getExpiringDocuments());
    }
}
