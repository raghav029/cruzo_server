package com.carbooking.dto.request.public_;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class ContactEnquiryRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String email;
    private String phone;

    @NotBlank(message = "Message is required")
    private String message;

    @NotNull(message = "tenantId is required")
    private UUID tenantId;
}
