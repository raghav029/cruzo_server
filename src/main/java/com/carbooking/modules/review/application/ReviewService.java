package com.carbooking.modules.review.application;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.DuplicateResourceException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Booking;
import com.carbooking.entity.Review;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.review.dto.request.CreateReviewRequest;
import com.carbooking.modules.review.dto.response.DriverRatingResponse;
import com.carbooking.modules.review.dto.response.ReviewResponse;
import com.carbooking.repository.BookingRepository;
import com.carbooking.repository.DriverRepository;
import com.carbooking.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService extends TenantSupport {

    private final ReviewRepository reviewRepo;
    private final BookingRepository bookingRepo;
    private final DriverRepository driverRepo;

    @Transactional
    public ReviewResponse submitReview(UUID bookingId, CreateReviewRequest request, String role) {
        Tenant tenant = requireTenant();

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        assertSameTenant(booking.getTenant().getId());

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessRuleException("Reviews can only be submitted for COMPLETED bookings");
        }

        if (reviewRepo.existsByBookingId(bookingId)) {
            throw new DuplicateResourceException("A review already exists for this booking");
        }

        String reviewerType;
        UUID reviewerUserId = null;
        UUID reviewerCustomerId = null;

        if ("ROLE_EMPLOYEE".equals(role)) {
            UUID userId = SecurityUtils.getCurrentUserId();
            if (booking.getEmployee() == null || !booking.getEmployee().getId().equals(userId)) {
                throw new UnauthorizedException("You can only review your own bookings");
            }
            reviewerType = "EMPLOYEE";
            reviewerUserId = userId;
        } else if ("ROLE_CUSTOMER".equals(role)) {
            UUID customerId = SecurityUtils.getCurrentCustomerId();
            if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(customerId)) {
                throw new UnauthorizedException("You can only review your own bookings");
            }
            reviewerType = "CUSTOMER";
            reviewerCustomerId = customerId;
        } else {
            throw new UnauthorizedException("Only employees and customers can submit reviews");
        }

        String tagsStr = (request.getTags() != null && !request.getTags().isEmpty())
                ? String.join(",", request.getTags())
                : null;

        Review review = Review.builder()
                .tenant(tenant)
                .booking(booking)
                .driver(booking.getDriver())
                .rating(request.getRating())
                .comment(request.getComment())
                .tags(tagsStr)
                .reviewerType(reviewerType)
                .reviewerUserId(reviewerUserId)
                .reviewerCustomerId(reviewerCustomerId)
                .build();

        return toResponse(reviewRepo.save(review));
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listReviews(Integer rating, Pageable pageable) {
        Tenant tenant = requireTenant();
        Page<Review> page = (rating != null)
                ? reviewRepo.findByTenantAndRating(tenant, rating, pageable)
                : reviewRepo.findByTenant(tenant, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listDriverReviews(UUID driverId, Pageable pageable) {
        requireTenant();
        driverRepo.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        return reviewRepo.findByDriverId(driverId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DriverRatingResponse getDriverRating(UUID driverId) {
        requireTenant();
        var driver = driverRepo.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        double avg = reviewRepo.averageRatingByDriverId(driverId);
        avg = Math.round(avg * 100.0) / 100.0;
        long count = reviewRepo.countByDriverId(driverId);

        return DriverRatingResponse.builder()
                .driverId(driverId)
                .driverName(driver.getUser().getFullName())
                .averageRating(avg)
                .reviewCount(count)
                .build();
    }

    private ReviewResponse toResponse(Review r) {
        List<String> tags = (r.getTags() != null && !r.getTags().isBlank())
                ? List.of(r.getTags().split(","))
                : List.of();
        return ReviewResponse.builder()
                .id(r.getId())
                .bookingId(r.getBooking().getId())
                .driverId(r.getDriver() != null ? r.getDriver().getId() : null)
                .driverName(r.getDriver() != null ? r.getDriver().getUser().getFullName() : null)
                .rating(r.getRating())
                .comment(r.getComment())
                .tags(tags)
                .reviewerType(r.getReviewerType())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
