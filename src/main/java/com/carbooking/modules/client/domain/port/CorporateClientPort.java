package com.carbooking.modules.client.domain.port;

import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CorporateClientPort {
    Optional<CorporateClient> findById(UUID id);
    Page<CorporateClient> findByTenant(Tenant tenant, Pageable pageable);
    boolean existsByTenantAndCompanyName(Tenant tenant, String companyName);
    CorporateClient save(CorporateClient client);
}
