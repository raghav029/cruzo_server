package com.carbooking.modules.booking.application;

import com.carbooking.entity.Addon;
import com.carbooking.entity.BookingAddon;
import com.carbooking.entity.Review;
import com.carbooking.modules.addon.domain.port.AddonPort;
import com.carbooking.modules.addon.domain.port.BookingAddonPort;
import com.carbooking.modules.addon.dto.response.BookingAddonResponse;
import com.carbooking.modules.booking.dto.response.BookingResponse;
import com.carbooking.modules.booking.dto.response.BookingStatusHistoryResponse;
import com.carbooking.modules.review.dto.response.ReviewResponse;
import com.carbooking.entity.Booking;
import com.carbooking.entity.BookingStatusHistory;
import com.carbooking.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookingMapper {

    private final ReviewRepository reviewRepository;
    private final BookingAddonPort bookingAddonRepository;
    private final AddonPort addonRepository;

    public BookingResponse toResponse(Booking booking) {
        ReviewResponse reviewResponse = reviewRepository.findByBookingId(booking.getId())
                .map(this::toReviewResponse)
                .orElse(null);

        return BookingResponse.builder()
                .id(booking.getId())
                .tenantId(booking.getTenant().getId())
                .corporateClientId(booking.getCorporateClient().getId())
                .corporateClientName(booking.getCorporateClient().getCompanyName())
                .employeeUserId(booking.getEmployee().getId())
                .employeeName(booking.getEmployee().getFullName())
                .driverId(booking.getDriver() != null ? booking.getDriver().getId() : null)
                .driverName(booking.getDriver() != null ? booking.getDriver().getUser().getFullName() : null)
                .driverPhone(booking.getDriver() != null ? booking.getDriver().getUser().getPhone() : null)
                .vehicleId(booking.getVehicle() != null ? booking.getVehicle().getId() : null)
                .vehiclePlate(booking.getVehicle() != null ? booking.getVehicle().getPlateNumber() : null)
                .assignmentMode(booking.getAssignmentMode())
                .pickupAddress(booking.getPickupAddress())
                .dropAddress(booking.getDropAddress())
                .pickupLat(booking.getPickupLat())
                .pickupLng(booking.getPickupLng())
                .dropLat(booking.getDropLat())
                .dropLng(booking.getDropLng())
                .vehicleTypeRequested(booking.getVehicleTypeRequested())
                .scheduledAt(booking.getScheduledAt())
                .notes(booking.getNotes())
                .occasion(booking.getOccasion())
                .status(booking.getStatus())
                .cancellationReason(booking.getCancellationReason())
                .rejectionReason(booking.getRejectionReason())
                .estimatedFare(booking.getEstimatedFare())
                .finalFare(booking.getFinalFare())
                .cancellationFee(booking.getCancellationFee())
                .approvedAt(booking.getApprovedAt())
                .driverAssignedAt(booking.getDriverAssignedAt())
                .tripStartedAt(booking.getTripStartedAt())
                .tripCompletedAt(booking.getTripCompletedAt())
                .cancelledAt(booking.getCancelledAt())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .driverCurrentLat(booking.getDriverCurrentLat())
                .driverCurrentLng(booking.getDriverCurrentLng())
                .locationUpdatedAt(booking.getLocationUpdatedAt())
                .boardingOtp(booking.getBoardingOtp())
                .dropOtp(booking.getDropOtp())
                .otpVerifiedAt(booking.getOtpVerifiedAt())
                .review(reviewResponse)
                .addons(mapAddons(booking.getId()))
                .build();
    }

    private ReviewResponse toReviewResponse(Review r) {
        List<String> tags = r.getTags() != null
                ? Arrays.stream(r.getTags().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toList())
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

    public BookingStatusHistoryResponse toHistoryResponse(BookingStatusHistory history) {
        return BookingStatusHistoryResponse.builder()
                .id(history.getId())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .actorUserId(history.getActor() != null ? history.getActor().getId() : null)
                .reason(history.getReason())
                .transitionedAt(history.getTransitionedAt())
                .build();
    }

    private List<BookingAddonResponse> mapAddons(java.util.UUID bookingId) {
        return bookingAddonRepository.findByBookingId(bookingId).stream()
                .map(ba -> {
                    Addon addon = addonRepository.findById(ba.getAddonId()).orElse(null);
                    return BookingAddonResponse.builder()
                            .addonId(ba.getAddonId())
                            .name(addon != null ? addon.getName() : null)
                            .quantity(ba.getQuantity())
                            .priceSnapshot(ba.getPriceSnapshot())
                            .total(ba.getPriceSnapshot().multiply(java.math.BigDecimal.valueOf(ba.getQuantity())))
                            .build();
                })
                .collect(Collectors.toList());
    }
}
