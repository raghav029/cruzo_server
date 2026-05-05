package com.carbooking.repository;

import com.carbooking.common.enums.SosStatus;
import com.carbooking.entity.SosAlert;
import com.carbooking.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SosAlertRepository extends JpaRepository<SosAlert, UUID> {
    Page<SosAlert> findByTenantOrderByCreatedAtDesc(Tenant tenant, Pageable pageable);
    Page<SosAlert> findByTenantAndStatus(Tenant tenant, SosStatus status, Pageable pageable);
    long countByTenantAndStatus(Tenant tenant, SosStatus status);
}
