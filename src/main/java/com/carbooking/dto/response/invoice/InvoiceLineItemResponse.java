package com.carbooking.dto.response.invoice;

import com.carbooking.common.enums.VehicleType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class InvoiceLineItemResponse {
    private UUID id;
    private UUID bookingId;
    private String description;
    private LocalDate tripDate;
    private VehicleType vehicleType;
    private BigDecimal baseFare;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal lineTotal;
}
