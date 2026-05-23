package com.carbooking.dto.request.vehicle;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddVehicleImageRequest {
    @NotBlank(message = "Image URL is required")
    private String imageUrl;
    private short displayOrder = 0;
}
