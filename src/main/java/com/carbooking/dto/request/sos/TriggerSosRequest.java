package com.carbooking.dto.request.sos;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class TriggerSosRequest {
    private UUID bookingId;
    private BigDecimal lat;
    private BigDecimal lng;
    private String message;
}
