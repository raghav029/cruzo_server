package com.carbooking.controller.tenant;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.dto.response.document.ExpiringDocumentsResponse;
import com.carbooking.service.DocumentExpiryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Document Expiry")
public class DocumentExpiryController {

    private final DocumentExpiryService documentExpiryService;

    @GetMapping("/expiring")
    public ResponseEntity<ApiResponse<ExpiringDocumentsResponse>> getExpiringDocuments() {
        return ResponseEntity.ok(ApiResponse.ok(documentExpiryService.getExpiringDocuments()));
    }
}
