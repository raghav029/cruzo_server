package com.carbooking.repository;

import com.carbooking.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    boolean existsBySubdomain(String subdomain);
    Optional<Tenant> findBySubdomain(String subdomain);
    Page<Tenant> findByActiveTrue(Pageable pageable);
}
