package com.carbooking.entity;

import com.carbooking.common.util.AuditableEntity;
import com.carbooking.entity.enums.BookingMode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tenants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tenant extends AuditableEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String subdomain;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;

    @Column(name = "support_email")
    private String supportEmail;

    @Column(name = "support_phone", length = 20)
    private String supportPhone;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private String timezone = "Asia/Kolkata";

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_mode", nullable = false, length = 20)
    private BookingMode bookingMode = BookingMode.CORPORATE;

    public boolean supportsB2C() {
        return bookingMode == BookingMode.B2C || bookingMode == BookingMode.BOTH;
    }

    public boolean supportsCorporate() {
        return bookingMode == BookingMode.CORPORATE || bookingMode == BookingMode.BOTH;
    }
}
