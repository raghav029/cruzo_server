package com.carbooking.dto.request.dailyschedule;

import com.carbooking.common.enums.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class UpdateDailyScheduleRequest {
    private String name;
    private VehicleType vehicleType;
    private List<String> recurrenceDays;
    private java.time.LocalTime pickupTime;
    private String dropAddress;
    private java.math.BigDecimal dropLat;
    private java.math.BigDecimal dropLng;
    private Boolean isPooled;
    @Min(1) @Max(12) private Integer maxCapacity;
}
