package com.carbooking.modules.dailyschedule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VerifyDailyTripOtpRequest {
    @NotBlank @Size(min = 4, max = 6) private String otp;
}
