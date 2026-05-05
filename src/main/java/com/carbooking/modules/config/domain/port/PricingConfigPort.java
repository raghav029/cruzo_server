package com.carbooking.modules.config.domain.port;

import com.carbooking.entity.PricingConfig;
import com.carbooking.entity.Tenant;

import java.util.Optional;

public interface PricingConfigPort {
    Optional<PricingConfig> findByTenant(Tenant tenant);
    PricingConfig save(PricingConfig config);
}
