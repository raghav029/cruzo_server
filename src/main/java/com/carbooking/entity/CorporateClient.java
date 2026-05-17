package com.carbooking.entity;

import com.carbooking.common.enums.BillingCycle;
import com.carbooking.common.util.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "corporate_clients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CorporateClient extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "billing_address")
    private String billingAddress;

    @Column(name = "billing_email", nullable = false)
    private String billingEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    @Column(name = "credit_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "current_outstanding", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentOutstanding = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "max_booking_value", precision = 12, scale = 2)
    private BigDecimal maxBookingValue;

    @Column(name = "allowed_vehicle_types")
    private String allowedVehicleTypes;
}
