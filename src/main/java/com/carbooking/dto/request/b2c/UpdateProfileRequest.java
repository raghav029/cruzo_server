package com.carbooking.dto.request.b2c;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateProfileRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Enter valid email")
    private String email;
}
