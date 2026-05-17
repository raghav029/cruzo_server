package com.carbooking.entity;

import com.carbooking.common.util.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "corporate_employees")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CorporateEmployee extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corporate_client_id", nullable = false)
    private CorporateClient corporateClient;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "employee_code")
    private String employeeCode;

    private String department;
    private String designation;

    @Column(name = "monthly_ride_limit")
    private Integer monthlyRideLimit;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "max_booking_value_override", precision = 12, scale = 2)
    private BigDecimal maxBookingValueOverride;

    @Column(name = "allowed_vehicle_types_override")
    private String allowedVehicleTypesOverride;
}
