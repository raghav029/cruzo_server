package com.carbooking.modules.review.controller;

import com.carbooking.common.response.ApiResponse;
import com.carbooking.common.response.PagedResponse;
import com.carbooking.common.util.ResponseHelper;
import com.carbooking.modules.review.application.ReviewService;
import com.carbooking.modules.review.dto.request.CreateReviewRequest;
import com.carbooking.modules.review.dto.response.DriverRatingResponse;
import com.carbooking.modules.review.dto.response.ReviewResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Reviews")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/bookings/{bookingId}/review")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @PathVariable UUID bookingId,
            @Valid @RequestBody CreateReviewRequest request,
            Authentication auth) {
        String role = auth.getAuthorities().iterator().next().getAuthority();
        return ResponseHelper.created(reviewService.submitReview(bookingId, request, role));
    }

    @GetMapping("/api/reviews")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> listReviews(
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseHelper.ok(new PagedResponse<>(
                reviewService.listReviews(rating, PageRequest.of(page, size))));
    }

    @GetMapping("/api/drivers/{driverId}/reviews")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> listDriverReviews(
            @PathVariable UUID driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseHelper.ok(new PagedResponse<>(
                reviewService.listDriverReviews(driverId, PageRequest.of(page, size))));
    }

    @GetMapping("/api/drivers/{driverId}/rating")
    public ResponseEntity<ApiResponse<DriverRatingResponse>> getDriverRating(
            @PathVariable UUID driverId) {
        return ResponseHelper.ok(reviewService.getDriverRating(driverId));
    }
}
