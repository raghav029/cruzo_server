package com.carbooking.modules.booking.application;

import com.carbooking.common.enums.BookingStatus;
import com.carbooking.common.enums.VehicleType;
import com.carbooking.common.exception.BusinessRuleException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.Booking;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.CorporateEmployee;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.booking.dto.request.CreateBookingRequest;
import com.carbooking.modules.booking.dto.response.BookingResponse;
import com.carbooking.modules.booking.application.BookingMapper;
import com.carbooking.modules.booking.domain.port.BookingPort;
import com.carbooking.modules.client.domain.port.CorporateClientPort;
import com.carbooking.modules.client.domain.port.CorporateEmployeePort;
import com.carbooking.modules.config.domain.port.PricingConfigPort;
import com.carbooking.modules.notification.application.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
public class BookingCreationService extends TenantSupport {

    private final BookingPort bookingRepository;
    private final CorporateClientPort corporateClientRepository;
    private final CorporateEmployeePort corporateEmployeeRepository;
    private final PricingConfigPort pricingConfigRepository;
    private final BookingHistoryHelper historyHelper;
    private final NotificationService notificationService;
    private final BookingMapper bookingMapper;

    public BookingCreationService(
            BookingPort bookingRepository,
            CorporateClientPort corporateClientRepository,
            CorporateEmployeePort corporateEmployeeRepository,
            PricingConfigPort pricingConfigRepository,
            BookingHistoryHelper historyHelper,
            NotificationService notificationService,
            BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.corporateClientRepository = corporateClientRepository;
        this.corporateEmployeeRepository = corporateEmployeeRepository;
        this.pricingConfigRepository = pricingConfigRepository;
        this.historyHelper = historyHelper;
        this.notificationService = notificationService;
        this.bookingMapper = bookingMapper;
    }

    @Transactional
    public BookingResponse create(CreateBookingRequest request) {
        Tenant tenant = requireTenant();

        CorporateClient client = corporateClientRepository.findById(request.getCorporateClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Corporate client not found"));
        assertSameTenant(client.getTenant().getId());

        if (request.getScheduledAt().isBefore(Instant.now().plus(2, ChronoUnit.HOURS))) {
            throw new BusinessRuleException("Booking must be scheduled at least 2 hours in advance");
        }

        // Travel policy enforcement (skip for FLEET_MANAGER and CORPORATE_ADMIN)
        String currentRole = SecurityUtils.getCurrentRole();
        if (!"FLEET_MANAGER".equals(currentRole) && !"CORPORATE_ADMIN".equals(currentRole)) {
            CorporateEmployee employee = corporateEmployeeRepository.findByUser(currentUser())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

            BigDecimal effectiveMax = employee.getMaxBookingValueOverride() != null
                    ? employee.getMaxBookingValueOverride()
                    : client.getMaxBookingValue();

            String effectiveTypes = employee.getAllowedVehicleTypesOverride() != null
                    ? employee.getAllowedVehicleTypesOverride()
                    : client.getAllowedVehicleTypes();

            if (effectiveTypes != null && !effectiveTypes.isBlank()) {
                List<String> allowed = Arrays.asList(effectiveTypes.split(","));
                if (!allowed.contains(request.getVehicleTypeRequested().name())) {
                    throw new BusinessRuleException(
                            "Vehicle type " + request.getVehicleTypeRequested() + " is not allowed by your travel policy");
                }
            }

            if (effectiveMax != null) {
                BigDecimal estimated = estimateFare(tenant, request.getVehicleTypeRequested());
                if (estimated != null && estimated.compareTo(effectiveMax) > 0) {
                    throw new BusinessRuleException(
                            "Estimated fare ₹" + estimated.setScale(0, java.math.RoundingMode.HALF_UP)
                            + " exceeds your travel policy limit of ₹" + effectiveMax.setScale(0, java.math.RoundingMode.HALF_UP));
                }
            }
        }

        Booking booking = Booking.builder()
                .tenant(tenant)
                .corporateClient(client)
                .employee(currentUser())
                .pickupAddress(request.getPickupAddress())
                .dropAddress(request.getDropAddress())
                .pickupLat(request.getPickupLat())
                .pickupLng(request.getPickupLng())
                .dropLat(request.getDropLat())
                .dropLng(request.getDropLng())
                .vehicleTypeRequested(request.getVehicleTypeRequested())
                .scheduledAt(request.getScheduledAt())
                .notes(request.getNotes())
                .status(BookingStatus.PENDING_APPROVAL)
                .estimatedFare(estimateFare(tenant, request.getVehicleTypeRequested()))
                .build();

        booking = bookingRepository.save(booking);
        historyHelper.append(booking, null, BookingStatus.PENDING_APPROVAL, booking.getEmployee(), null);
        notificationService.notifyBookingCreated(booking);

        return bookingMapper.toResponse(booking);
    }

    private BigDecimal estimateFare(Tenant tenant, VehicleType type) {
        return pricingConfigRepository.findByTenant(tenant)
                .map(config -> {
                    BigDecimal multiplier = switch (type) {
                        case SEDAN -> config.getSedanMultiplier();
                        case SUV -> config.getSuvMultiplier();
                        case LUXURY -> config.getLuxuryMultiplier();
                    };
                    return config.getBaseFare().multiply(multiplier).max(config.getMinimumFare());
                })
                .orElse(null);
    }
}
