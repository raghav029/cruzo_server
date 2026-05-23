package com.carbooking.dto.request.city;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateCityRequest {
    @NotBlank(message = "City name is required")
    private String name;
}
