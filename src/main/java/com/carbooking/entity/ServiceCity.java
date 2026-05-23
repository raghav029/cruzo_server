package com.carbooking.entity;

import com.carbooking.common.util.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_cities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceCity extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active = true;
}
