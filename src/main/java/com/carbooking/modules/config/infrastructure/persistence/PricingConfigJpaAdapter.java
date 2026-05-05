package com.carbooking.modules.config.infrastructure.persistence;

import com.carbooking.entity.PricingConfig;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.config.domain.port.PricingConfigPort;
import com.carbooking.repository.PricingConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PricingConfigJpaAdapter implements PricingConfigPort {

    private final PricingConfigRepository repo;

    @Override public Optional<PricingConfig> findByTenant(Tenant tenant) { return repo.findByTenant(tenant); }
    @Override public PricingConfig save(PricingConfig config) { return repo.save(config); }
}
