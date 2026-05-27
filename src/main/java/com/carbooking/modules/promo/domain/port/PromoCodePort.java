package com.carbooking.modules.promo.domain.port;

import com.carbooking.entity.PromoCode;
import com.carbooking.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface PromoCodePort {
    PromoCode save(PromoCode promoCode);
    Optional<PromoCode> findById(UUID id);
    Optional<PromoCode> findByTenantAndCodeIgnoreCase(Tenant tenant, String code);
    Page<PromoCode> findByTenant(Tenant tenant, Pageable pageable);
    boolean existsByTenantAndCodeIgnoreCase(Tenant tenant, String code);
}
