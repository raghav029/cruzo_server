package com.carbooking.modules.dailyschedule.dto.request;

import com.carbooking.common.enums.VehicleType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter @Setter
public class CreateDailyScheduleRequest {
    @NotBlank private String name;
    @NotNull private UUID corporateClientId;
    @NotNull private VehicleType vehicleType;
    @NotEmpty private List<String> recurrenceDays; // ["MON","TUE","WED","THU","FRI"]
    @NotNull private java.time.LocalTime pickupTime;
    @NotBlank private String dropAddress;
    private java.math.BigDecimal dropLat;
    private java.math.BigDecimal dropLng;
    private boolean isPooled = false;
    @Min(1) @Max(12) private Integer maxCapacity = 1;
}
