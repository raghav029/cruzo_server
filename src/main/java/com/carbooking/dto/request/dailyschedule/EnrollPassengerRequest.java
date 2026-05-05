package com.carbooking.dto.request.dailyschedule;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EnrollPassengerRequest {
    @Email @NotBlank private String employeeEmail;
    @NotBlank private String pickupAddress;
    private java.math.BigDecimal pickupLat;
    private java.math.BigDecimal pickupLng;
}
