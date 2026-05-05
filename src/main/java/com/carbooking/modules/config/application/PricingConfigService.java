package com.carbooking.modules.config.application;

import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.PricingConfig;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.config.dto.request.UpdatePricingConfigRequest;
import com.carbooking.modules.config.dto.response.PricingConfigResponse;
import com.carbooking.modules.config.domain.port.PricingConfigPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import com.carbooking.modules.config.application.PricingConfigMapper;

@Service
public class PricingConfigService extends TenantSupport {

    private final PricingConfigMapper pricingMapper;
    private final PricingConfigPort pricingConfigRepository;

    public PricingConfigService(PricingConfigPort pricingConfigRepository,
                                 PricingConfigMapper pricingMapper) {
        this.pricingConfigRepository = pricingConfigRepository;
        this.pricingMapper = pricingMapper;
    }

    @Transactional(readOnly = true)
    public PricingConfigResponse get() {
        return pricingMapper.toResponse(findOrCreate());
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

        return pricingMapper.toResponse(pricingConfigRepository.save(config));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private PricingConfig findOrCreate() {
        Tenant tenant = requireTenant();
        return pricingConfigRepository.findByTenant(tenant)
                .orElseGet(() -> pricingConfigRepository.save(
                        PricingConfig.builder()
                                .tenant(tenant)
                                .effectiveFrom(Instant.now())
                                .build()));
    }

}
