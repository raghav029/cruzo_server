package com.carbooking.modules.booking.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class LiveTripResponse {
    private UUID bookingId;
    private String vehicleName;
    private String plateNumber;
    private String driverName;
    private String driverPhone;
    private String passengerName;
    private String pickupAddress;
    private String dropAddress;
    private BigDecimal driverLat;
    private BigDecimal driverLng;
    private Instant locationUpdatedAt;
    private Instant scheduledAt;
}
