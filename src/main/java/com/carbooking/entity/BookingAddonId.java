package com.carbooking.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class BookingAddonId implements Serializable {
    private UUID bookingId;
    private UUID addonId;
}
