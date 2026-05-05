package com.carbooking.modules.sos.infrastructure.persistence;

import com.carbooking.common.enums.SosStatus;
import com.carbooking.entity.SosAlert;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.sos.domain.port.SosAlertPort;
import com.carbooking.repository.SosAlertRepository;
import com.carbooking.modules.sos.domain.port.SosAlertPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SosAlertJpaAdapter implements SosAlertPort {

    private final SosAlertRepository repo;

    @Override public Optional<SosAlert> findById(UUID id) { return repo.findById(id); }
    @Override public Page<SosAlert> findByTenantOrderByCreatedAtDesc(Tenant tenant, Pageable pageable) { return repo.findByTenantOrderByCreatedAtDesc(tenant, pageable); }
    @Override public Page<SosAlert> findByTenantAndStatus(Tenant tenant, SosStatus status, Pageable pageable) { return repo.findByTenantAndStatus(tenant, status, pageable); }
    @Override public long countByTenantAndStatus(Tenant tenant, SosStatus status) { return repo.countByTenantAndStatus(tenant, status); }
    @Override public SosAlert save(SosAlert alert) { return repo.save(alert); }
}
