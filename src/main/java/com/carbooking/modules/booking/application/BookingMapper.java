package com.carbooking.modules.booking.application;

import com.carbooking.modules.booking.dto.response.BookingResponse;
import com.carbooking.modules.booking.dto.response.BookingStatusHistoryResponse;
import com.carbooking.entity.Booking;
import com.carbooking.entity.BookingStatusHistory;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
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
}
