package com.carbooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "booking_addons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(BookingAddonId.class)
public class BookingAddon {

    @Id
    @Column(name = "booking_id")
    private UUID bookingId;

    @Id
    @Column(name = "addon_id")
    private UUID addonId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", insertable = false, updatable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addon_id", insertable = false, updatable = false)
    private Addon addon;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(name = "price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceSnapshot;
}
