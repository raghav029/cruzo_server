package com.carbooking.modules.config.application;

import com.carbooking.entity.PricingConfig;
import com.carbooking.modules.config.dto.response.PricingConfigResponse;
import org.springframework.stereotype.Component;

@Component
public class PricingConfigMapper {

    public PricingConfigResponse toResponse(PricingConfig c) {
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
