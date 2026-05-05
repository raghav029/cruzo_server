package com.carbooking.dto.request.driver;

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
