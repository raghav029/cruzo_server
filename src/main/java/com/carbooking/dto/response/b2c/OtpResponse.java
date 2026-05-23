package com.carbooking.dto.response.b2c;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class OtpResponse {
    private String maskedPhone;
    private int expiresInSeconds;
}
