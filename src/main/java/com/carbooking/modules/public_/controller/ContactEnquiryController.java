package com.carbooking.modules.public_.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.dto.request.public_.ContactEnquiryRequest;
import com.carbooking.service.ContactEnquiryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Public — Contact")
@RestController
@RequestMapping("/api/public/enquiries")
@RequiredArgsConstructor
public class ContactEnquiryController {

    private final ContactEnquiryService contactEnquiryService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submit(
            @Valid @RequestBody ContactEnquiryRequest req) {
        contactEnquiryService.submit(req);
        return ResponseHelper.ok(null);
    }
}
