package com.carbooking.repository;

import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CorporateClientRepository extends JpaRepository<CorporateClient, UUID> {
    Page<CorporateClient> findByTenant(Tenant tenant, Pageable pageable);
    boolean existsByTenantAndCompanyName(Tenant tenant, String companyName);
}
