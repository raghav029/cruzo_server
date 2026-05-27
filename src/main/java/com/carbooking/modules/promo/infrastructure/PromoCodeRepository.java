package com.carbooking.modules.promo.infrastructure;

import com.carbooking.entity.PromoCode;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.promo.domain.port.PromoCodePort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID>, PromoCodePort {
    Optional<PromoCode> findByTenantAndCodeIgnoreCase(Tenant tenant, String code);
    Page<PromoCode> findByTenant(Tenant tenant, Pageable pageable);
    boolean existsByTenantAndCodeIgnoreCase(Tenant tenant, String code);
}
