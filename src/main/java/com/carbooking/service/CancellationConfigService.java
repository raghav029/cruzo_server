package com.carbooking.service;

import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.request.config.UpdateCancellationConfigRequest;
import com.carbooking.dto.response.config.CancellationConfigResponse;
import com.carbooking.entity.CancellationConfig;
import com.carbooking.entity.Tenant;
import com.carbooking.repository.CancellationConfigRepository;
import com.carbooking.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancellationConfigService {

    private final CancellationConfigRepository cancellationConfigRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public CancellationConfigResponse get() {
        return toResponse(findOrCreate());
    }

    @Transactional
    public CancellationConfigResponse update(UpdateCancellationConfigRequest request) {
        CancellationConfig config = findOrCreate();

        if (request.getCancellationWindowHours() != null) config.setCancellationWindowHours(request.getCancellationWindowHours());
        if (request.getFeeType() != null) config.setFeeType(request.getFeeType());
        if (request.getFeeValue() != null) config.setFeeValue(request.getFeeValue());
        if (request.getAfterWindowAllowed() != null) config.setAfterWindowAllowed(request.getAfterWindowAllowed());

        return toResponse(cancellationConfigRepository.save(config));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CancellationConfig findOrCreate() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        return cancellationConfigRepository.findByTenant(tenant)
                .orElseGet(() -> cancellationConfigRepository.save(
                        CancellationConfig.builder().tenant(tenant).build()));
    }

    private CancellationConfigResponse toResponse(CancellationConfig c) {
        return CancellationConfigResponse.builder()
                .id(c.getId())
                .tenantId(c.getTenant().getId())
                .cancellationWindowHours(c.getCancellationWindowHours())
                .feeType(c.getFeeType())
                .feeValue(c.getFeeValue())
                .afterWindowAllowed(c.isAfterWindowAllowed())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
