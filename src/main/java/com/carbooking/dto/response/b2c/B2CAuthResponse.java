package com.carbooking.dto.response.b2c;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class B2CAuthResponse {
    private String token;
    private CustomerResponse customer;
}
