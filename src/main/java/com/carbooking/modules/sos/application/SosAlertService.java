package com.carbooking.modules.sos.application;

import com.carbooking.common.enums.SosStatus;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Booking;
import com.carbooking.entity.SosAlert;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.modules.notification.application.NotificationService;
import com.carbooking.modules.sos.dto.request.ResolveSosRequest;
import com.carbooking.modules.sos.dto.request.TriggerSosRequest;
import com.carbooking.modules.sos.dto.response.SosAlertResponse;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.sos.domain.port.SosAlertPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import com.carbooking.modules.sos.application.SosAlertMapper;

@Service
public class SosAlertService extends TenantSupport {

    private final SosAlertMapper sosMapper;
    private final SosAlertPort sosAlertRepository;
    private final BookingPort bookingRepository;
    private final NotificationService notificationService;

    public SosAlertService(SosAlertPort sosAlertRepository,
                           BookingPort bookingRepository,
                           @Lazy NotificationService notificationService,
                                 SosAlertMapper sosMapper) {
        this.sosAlertRepository = sosAlertRepository;
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
        this.sosMapper = sosMapper;
    }

    @Transactional
    public SosAlertResponse trigger(TriggerSosRequest request) {
        Tenant tenant = requireTenant();
        User triggeredBy = currentUser();

        Booking booking = null;
        if (request.getBookingId() != null) {
            booking = bookingRepository.findById(request.getBookingId()).orElse(null);
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

        String bookingRef = booking != null ? booking.getId().toString().substring(0, 8).toUpperCase() : "N/A";
        String smsMsg = String.format(
                "SOS ALERT from %s at %s,%s. Booking: %s. Message: %s",
                triggeredBy.getFullName(),
                request.getLat() != null ? request.getLat().toPlainString() : "N/A",
                request.getLng() != null ? request.getLng().toPlainString() : "N/A",
                bookingRef,
                request.getMessage() != null ? request.getMessage() : "N/A"
        );
        notificationService.sendSosAlert(tenant, smsMsg);

        return sosMapper.toResponse(alert);
    }

    @Transactional
    public SosAlertResponse resolve(UUID alertId, ResolveSosRequest request) {
        SosAlert alert = findAlert(alertId);
        User resolver = currentUser();

        alert.setStatus(SosStatus.RESOLVED);
        alert.setResolvedBy(resolver);
        alert.setResolvedAt(Instant.now());
        if (request.getNotes() != null) {
            alert.setMessage((alert.getMessage() != null ? alert.getMessage() + " | Resolution: " : "Resolution: ") + request.getNotes());
        }

        return sosMapper.toResponse(sosAlertRepository.save(alert));
    }

    @Transactional(readOnly = true)
    public Page<SosAlertResponse> list(SosStatus status, Pageable pageable) {
        Tenant tenant = requireTenant();
        Page<SosAlert> page = (status != null)
                ? sosAlertRepository.findByTenantAndStatus(tenant, status, pageable)
                : sosAlertRepository.findByTenantOrderByCreatedAtDesc(tenant, pageable);
        return page.map(sosMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SosAlertResponse get(UUID alertId) {
        return sosMapper.toResponse(findAlert(alertId));
    }

    private SosAlert findAlert(UUID alertId) {
        SosAlert alert = sosAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("SOS alert not found: " + alertId));
        assertSameTenant(alert.getTenant().getId());
        return alert;
    }

}
