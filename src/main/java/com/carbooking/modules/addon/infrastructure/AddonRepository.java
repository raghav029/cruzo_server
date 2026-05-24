package com.carbooking.modules.addon.infrastructure;

import com.carbooking.entity.Addon;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.addon.domain.port.AddonPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AddonRepository extends JpaRepository<Addon, UUID>, AddonPort {
    Page<Addon> findByTenant(Tenant tenant, Pageable pageable);
    List<Addon> findByTenantAndActiveTrue(Tenant tenant);
}
