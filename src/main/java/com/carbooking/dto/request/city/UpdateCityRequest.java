package com.carbooking.dto.request.city;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateCityRequest {
    private String name;
    private Boolean active;
}
