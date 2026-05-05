package com.carbooking.service;

import com.carbooking.common.enums.SosStatus;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.request.sos.ResolveSosRequest;
import com.carbooking.dto.request.sos.TriggerSosRequest;
import com.carbooking.dto.response.sos.SosAlertResponse;
import com.carbooking.entity.Booking;
import com.carbooking.entity.SosAlert;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.repository.BookingRepository;
import com.carbooking.repository.SosAlertRepository;
import com.carbooking.repository.TenantRepository;
import com.carbooking.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SosAlertService {

    private final SosAlertRepository sosAlertRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    public SosAlertService(SosAlertRepository sosAlertRepository,
                           TenantRepository tenantRepository,
                           UserRepository userRepository,
                           BookingRepository bookingRepository,
                           @Lazy NotificationService notificationService) {
        this.sosAlertRepository = sosAlertRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public SosAlertResponse trigger(TriggerSosRequest request) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        UUID userId = SecurityUtils.getCurrentUserId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        User triggeredBy = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Booking booking = null;
        if (request.getBookingId() != null) {
            booking = bookingRepository.findById(request.getBookingId())
                    .orElse(null);
        }

        SosAlert alert = SosAlert.builder()
                .tenant(tenant)
                .booking(booking)
                .triggeredBy(triggeredBy)
                .lat(request.getLat())
                .lng(request.getLng())
                .message(request.getMessage())
                .status(SosStatus.ACTIVE)
                .build();

        alert = sosAlertRepository.save(alert);

        // Notify fleet managers
        String bookingRef = booking != null ? booking.getId().toString().substring(0, 8).toUpperCase() : "N/A";
        String smsMsg = String.format(
                "🚨 SOS ALERT from %s at %s,%s. Booking: %s. Message: %s",
                triggeredBy.getFullName(),
                request.getLat() != null ? request.getLat().toPlainString() : "N/A",
                request.getLng() != null ? request.getLng().toPlainString() : "N/A",
                bookingRef,
                request.getMessage() != null ? request.getMessage() : "N/A"
        );
        notificationService.sendSosAlert(tenant, smsMsg);

        return toResponse(alert);
    }

    @Transactional
    public SosAlertResponse resolve(UUID alertId, ResolveSosRequest request) {
        SosAlert alert = findAndVerify(alertId);
        UUID userId = SecurityUtils.getCurrentUserId();
        User resolver = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        alert.setStatus(SosStatus.RESOLVED);
        alert.setResolvedBy(resolver);
        alert.setResolvedAt(Instant.now());
        if (request.getNotes() != null) {
            alert.setMessage((alert.getMessage() != null ? alert.getMessage() + " | Resolution: " : "Resolution: ") + request.getNotes());
        }

        return toResponse(sosAlertRepository.save(alert));
    }

    @Transactional(readOnly = true)
    public Page<SosAlertResponse> list(SosStatus status, Pageable pageable) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        Page<SosAlert> page = (status != null)
                ? sosAlertRepository.findByTenantAndStatus(tenant, status, pageable)
                : sosAlertRepository.findByTenantOrderByCreatedAtDesc(tenant, pageable);

        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SosAlertResponse get(UUID alertId) {
        return toResponse(findAndVerify(alertId));
    }

    private SosAlert findAndVerify(UUID alertId) {
        SosAlert alert = sosAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("SOS alert not found: " + alertId));
        if (!alert.getTenant().getId().equals(SecurityUtils.getCurrentTenantId())) {
            throw new UnauthorizedException("Access denied");
        }
        return alert;
    }

    private SosAlertResponse toResponse(SosAlert a) {
        return SosAlertResponse.builder()
                .id(a.getId())
                .tenantId(a.getTenant().getId())
                .bookingId(a.getBooking() != null ? a.getBooking().getId() : null)
                .triggeredByUserId(a.getTriggeredBy().getId())
                .triggeredByName(a.getTriggeredBy().getFullName())
                .lat(a.getLat())
                .lng(a.getLng())
                .message(a.getMessage())
                .status(a.getStatus())
                .resolvedByUserId(a.getResolvedBy() != null ? a.getResolvedBy().getId() : null)
                .resolvedAt(a.getResolvedAt())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
