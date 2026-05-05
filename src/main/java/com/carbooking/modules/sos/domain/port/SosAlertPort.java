package com.carbooking.modules.sos.domain.port;

import com.carbooking.common.enums.SosStatus;
import com.carbooking.entity.SosAlert;
import com.carbooking.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface SosAlertPort {
    Optional<SosAlert> findById(UUID id);
    Page<SosAlert> findByTenantOrderByCreatedAtDesc(Tenant tenant, Pageable pageable);
    Page<SosAlert> findByTenantAndStatus(Tenant tenant, SosStatus status, Pageable pageable);
    long countByTenantAndStatus(Tenant tenant, SosStatus status);
    SosAlert save(SosAlert alert);
}
