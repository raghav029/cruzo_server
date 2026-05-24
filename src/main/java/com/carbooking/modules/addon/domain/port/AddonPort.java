package com.carbooking.modules.addon.domain.port;

import com.carbooking.entity.Addon;
import com.carbooking.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddonPort {
    Addon save(Addon addon);
    Optional<Addon> findById(UUID id);
    Page<Addon> findByTenant(Tenant tenant, Pageable pageable);
    List<Addon> findByTenantAndActiveTrue(Tenant tenant);
    void deleteById(UUID id);
}
