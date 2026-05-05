package com.carbooking.modules.config.domain.port;

import com.carbooking.entity.CancellationConfig;
import com.carbooking.entity.Tenant;

import java.util.Optional;

public interface CancellationConfigPort {
    Optional<CancellationConfig> findByTenant(Tenant tenant);
    CancellationConfig save(CancellationConfig config);
}
