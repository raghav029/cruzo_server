package com.carbooking.modules.client.infrastructure.persistence;

import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.client.domain.port.CorporateClientPort;
import com.carbooking.repository.CorporateClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CorporateClientJpaAdapter implements CorporateClientPort {

    private final CorporateClientRepository repo;

    @Override public Optional<CorporateClient> findById(UUID id) { return repo.findById(id); }
    @Override public Page<CorporateClient> findByTenant(Tenant tenant, Pageable pageable) { return repo.findByTenant(tenant, pageable); }
    @Override public boolean existsByTenantAndCompanyName(Tenant tenant, String companyName) { return repo.existsByTenantAndCompanyName(tenant, companyName); }
    @Override public CorporateClient save(CorporateClient client) { return repo.save(client); }
}
