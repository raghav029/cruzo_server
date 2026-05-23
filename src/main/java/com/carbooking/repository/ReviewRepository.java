package com.carbooking.repository;

import com.carbooking.entity.Review;
import com.carbooking.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByBookingId(UUID bookingId);

    Optional<Review> findByBookingId(UUID bookingId);

    Page<Review> findByTenant(Tenant tenant, Pageable pageable);

    Page<Review> findByTenantAndRating(Tenant tenant, int rating, Pageable pageable);

    Page<Review> findByDriverId(UUID driverId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.driver.id = :driverId")
    double averageRatingByDriverId(@Param("driverId") UUID driverId);

    long countByDriverId(UUID driverId);
}
