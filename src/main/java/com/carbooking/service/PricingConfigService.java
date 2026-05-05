package com.carbooking.service;

import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.request.config.UpdatePricingConfigRequest;
import com.carbooking.dto.response.config.PricingConfigResponse;
import com.carbooking.entity.PricingConfig;
import com.carbooking.entity.Tenant;
import com.carbooking.repository.PricingConfigRepository;
import com.carbooking.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingConfigService {

    private final PricingConfigRepository pricingConfigRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public PricingConfigResponse get() {
        return toResponse(findOrCreate());
    }

    @Transactional
    public PricingConfigResponse update(UpdatePricingConfigRequest request) {
        PricingConfig config = findOrCreate();

        if (request.getBaseFare() != null) config.setBaseFare(request.getBaseFare());
        if (request.getPerKmRate() != null) config.setPerKmRate(request.getPerKmRate());
        if (request.getPerHourRate() != null) config.setPerHourRate(request.getPerHourRate());
        if (request.getMinimumFare() != null) config.setMinimumFare(request.getMinimumFare());
        if (request.getSedanMultiplier() != null) config.setSedanMultiplier(request.getSedanMultiplier());
        if (request.getSuvMultiplier() != null) config.setSuvMultiplier(request.getSuvMultiplier());
        if (request.getLuxuryMultiplier() != null) config.setLuxuryMultiplier(request.getLuxuryMultiplier());
        if (request.getCgstPct() != null) config.setCgstPct(request.getCgstPct());
        if (request.getSgstPct() != null) config.setSgstPct(request.getSgstPct());
        if (request.getEffectiveFrom() != null) config.setEffectiveFrom(request.getEffectiveFrom());

        return toResponse(pricingConfigRepository.save(config));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private PricingConfig findOrCreate() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        return pricingConfigRepository.findByTenant(tenant)
                .orElseGet(() -> pricingConfigRepository.save(
                        PricingConfig.builder()
                                .tenant(tenant)
                                .effectiveFrom(Instant.now())
                                .build()));
    }

    private PricingConfigResponse toResponse(PricingConfig c) {
        return PricingConfigResponse.builder()
                .id(c.getId())
                .tenantId(c.getTenant().getId())
                .baseFare(c.getBaseFare())
                .perKmRate(c.getPerKmRate())
                .perHourRate(c.getPerHourRate())
                .minimumFare(c.getMinimumFare())
                .sedanMultiplier(c.getSedanMultiplier())
                .suvMultiplier(c.getSuvMultiplier())
                .luxuryMultiplier(c.getLuxuryMultiplier())
                .cgstPct(c.getCgstPct())
                .sgstPct(c.getSgstPct())
                .active(c.isActive())
                .effectiveFrom(c.getEffectiveFrom())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
