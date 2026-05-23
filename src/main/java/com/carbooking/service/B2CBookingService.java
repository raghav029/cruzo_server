package com.carbooking.service;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.dto.request.b2c.CancelB2CBookingRequest;
import com.carbooking.dto.request.b2c.CreateB2CBookingRequest;
import com.carbooking.dto.response.b2c.B2CBookingResponse;
import com.carbooking.dto.response.vehicle.VehiclePackageResponse;
import com.carbooking.entity.*;
import com.carbooking.entity.enums.BookingType;
import com.carbooking.modules.promo.application.PromoCodeService;
import com.carbooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class B2CBookingService {

    private final BookingRepository bookingRepo;
    private final CustomerRepository customerRepo;
    private final VehicleRepository vehicleRepo;
    private final VehiclePackageRepository packageRepo;
    private final BookingStatusHistoryRepository statusHistoryRepo;
    private final B2CPricingEngine pricingEngine;
    private final PromoCodeService promoCodeService;

    @Transactional
    public B2CBookingResponse create(UUID customerId, CreateB2CBookingRequest req) {
        Customer customer = customerRepo.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (!customer.getTenant().supportsB2C())
            throw new BusinessRuleException("Tenant does not support B2C bookings");

        Vehicle vehicle = vehicleRepo.findById(req.getVehicleId())
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        if (!vehicle.isPublished())
            throw new BusinessRuleException("Vehicle is not available for booking");

        if (!vehicle.getTenant().getId().equals(customer.getTenant().getId()))
            throw new BusinessRuleException("Vehicle does not belong to your tenant");

        VehiclePackage pkg = packageRepo.findById(req.getPackageId())
            .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        if (!pkg.getVehicle().getId().equals(vehicle.getId()))
            throw new BusinessRuleException("Package does not belong to the selected vehicle");

        if (req.getScheduledAt().isBefore(Instant.now().plus(2, ChronoUnit.HOURS)))
            throw new BusinessRuleException("Booking must be at least 2 hours in advance");

        BigDecimal estimatedFare = pricingEngine.estimateFare(pkg);
        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedPromoCode = null;

        if (req.getPromoCode() != null && !req.getPromoCode().isBlank()) {
            discountAmount = promoCodeService.applyPromo(customer.getTenant(), req.getPromoCode(), estimatedFare);
            appliedPromoCode = req.getPromoCode().toUpperCase();
            estimatedFare = estimatedFare.subtract(discountAmount);
        }

        Booking booking = Booking.builder()
            .tenant(customer.getTenant())
            .customer(customer)
            .vehicle(vehicle)
            .vehiclePackage(pkg)
            .bookingType(BookingType.B2C)
            .vehicleTypeRequested(vehicle.getVehicleType())
            .pickupAddress(req.getPickupAddress())
            .dropAddress(req.getDropAddress())
            .scheduledAt(req.getScheduledAt())
            .notes(req.getNotes())
            .status(BookingStatus.PENDING_APPROVAL)
            .estimatedFare(estimatedFare)
            .promoCode(appliedPromoCode)
            .discountAmount(discountAmount.compareTo(BigDecimal.ZERO) > 0 ? discountAmount : null)
            .cityId(req.getCityId())
            .build();

        bookingRepo.save(booking);
        saveHistory(booking, null, BookingStatus.PENDING_APPROVAL);

        return toResponse(booking);
    }

    public Page<B2CBookingResponse> list(UUID customerId, Pageable pageable) {
        return bookingRepo
            .findByCustomerIdAndBookingType(customerId, BookingType.B2C, pageable)
            .map(this::toResponse);
    }

    public B2CBookingResponse get(UUID bookingId, UUID customerId) {
        return toResponse(findCustomerBooking(bookingId, customerId));
    }

    @Transactional
    public B2CBookingResponse cancel(UUID bookingId, UUID customerId,
                                     CancelB2CBookingRequest req) {
        Booking booking = findCustomerBooking(bookingId, customerId);

        if (booking.getStatus() == BookingStatus.COMPLETED
                || booking.getStatus() == BookingStatus.CANCELLED_BY_EMPLOYEE
                || booking.getStatus() == BookingStatus.CANCELLED_BY_ADMIN
                || booking.getStatus() == BookingStatus.CANCELLED_BY_FLEET_MANAGER
                || booking.getStatus() == BookingStatus.CANCELLED_BY_DRIVER)
            throw new BusinessRuleException("Cannot cancel booking in status: " + booking.getStatus());

        BookingStatus prevStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED_BY_EMPLOYEE);
        booking.setCancellationReason(req.getReason());
        bookingRepo.save(booking);
        saveHistory(booking, prevStatus, BookingStatus.CANCELLED_BY_EMPLOYEE);

        return toResponse(booking);
    }

    private Booking findCustomerBooking(UUID bookingId, UUID customerId) {
        Booking booking = bookingRepo.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(customerId))
            throw new BusinessRuleException("Access denied");
        return booking;
    }

    private void saveHistory(Booking booking, BookingStatus fromStatus, BookingStatus toStatus) {
        statusHistoryRepo.save(BookingStatusHistory.builder()
            .tenant(booking.getTenant())
            .booking(booking)
            .fromStatus(fromStatus)
            .toStatus(toStatus)
            .transitionedAt(Instant.now())
            .build());
    }

    private B2CBookingResponse toResponse(Booking b) {
        VehiclePackage pkg = b.getVehiclePackage();
        VehiclePackageResponse pkgDto = pkg == null ? null : VehiclePackageResponse.builder()
            .id(pkg.getId()).name(pkg.getName()).baseRental(pkg.getBaseRental())
            .includedKm(pkg.getIncludedKm()).includedHours(pkg.getIncludedHours())
            .extraPerKm(pkg.getExtraPerKm()).extraPerHour(pkg.getExtraPerHour())
            .driveBatta(pkg.getDriveBatta()).outstationBatta(pkg.getOutstationBatta())
            .nightBatta(pkg.getNightBatta()).build();

        Vehicle v = b.getVehicle();
        String thumbnail = (v != null && !v.getImages().isEmpty())
            ? v.getImages().get(0).getImageUrl() : null;

        return B2CBookingResponse.builder()
            .id(b.getId())
            .vehicleMake(v != null ? v.getMake() : null)
            .vehicleModel(v != null ? v.getModel() : null)
            .vehicleColor(v != null ? v.getColor() : null)
            .thumbnailUrl(thumbnail)
            .packageDetails(pkgDto)
            .pickupAddress(b.getPickupAddress())
            .dropAddress(b.getDropAddress())
            .scheduledAt(b.getScheduledAt())
            .status(b.getStatus())
            .notes(b.getNotes())
            .baseRental(pkg != null ? pkg.getBaseRental() : null)
            .extraKm(b.getExtraKm())
            .extraHours(b.getExtraHours())
            .driveBattaApplied(b.getDriveBattaApplied())
            .outstationBattaApplied(b.getOutstationBattaApplied())
            .nightBattaApplied(b.getNightBattaApplied())
            .parkingFee(b.getParkingFee())
            .tollFee(b.getTollFee())
            .gstAmount(b.getGstAmount())
            .estimatedFare(b.getEstimatedFare())
            .discountAmount(b.getDiscountAmount())
            .finalFare(b.getFinalFare())
            .promoCode(b.getPromoCode())
            .createdAt(b.getCreatedAt())
            .build();
    }
}
