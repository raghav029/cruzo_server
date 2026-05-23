package com.carbooking.entity;

import com.carbooking.common.util.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(length = 500)
    private String tags;

    @Column(name = "reviewer_type", nullable = false, length = 20)
    private String reviewerType;

    @Column(name = "reviewer_user_id")
    private UUID reviewerUserId;

    @Column(name = "reviewer_customer_id")
    private UUID reviewerCustomerId;
}
