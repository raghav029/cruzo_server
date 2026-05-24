package com.carbooking.modules.addon.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter @Setter
public class AddBookingAddonsRequest {

    @NotEmpty(message = "Addons list cannot be empty")
    private List<AddonItem> addons;

    @Getter @Setter
    public static class AddonItem {
        private UUID addonId;
        private int quantity = 1;
    }
}
