package com.carbooking.modules.config.infrastructure.persistence;

import com.carbooking.entity.CancellationConfig;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.config.domain.port.CancellationConfigPort;
import com.carbooking.repository.CancellationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CancellationConfigJpaAdapter implements CancellationConfigPort {

    private final CancellationConfigRepository repo;

    @Override public Optional<CancellationConfig> findByTenant(Tenant tenant) { return repo.findByTenant(tenant); }
    @Override public CancellationConfig save(CancellationConfig config) { return repo.save(config); }
}
