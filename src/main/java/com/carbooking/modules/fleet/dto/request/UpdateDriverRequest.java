package com.carbooking.modules.fleet.dto.request;

import com.carbooking.common.enums.DriverAvailability;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateDriverRequest {

    private String phone;
    private String licenseNumber;
    private LocalDate licenseExpiry;
    private LocalDate insuranceExpiry;
    private DriverAvailability availability;
    private Boolean active;
}
