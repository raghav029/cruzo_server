package com.carbooking.modules.config.application;

import com.carbooking.common.util.TenantSupport;
import com.carbooking.entity.CancellationConfig;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.config.dto.request.UpdateCancellationConfigRequest;
import com.carbooking.modules.config.dto.response.CancellationConfigResponse;
import com.carbooking.modules.config.domain.port.CancellationConfigPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carbooking.modules.config.application.CancellationConfigMapper;

@Service
public class CancellationConfigService extends TenantSupport {

    private final CancellationConfigMapper cancellationMapper;
    private final CancellationConfigPort cancellationConfigRepository;

    public CancellationConfigService(CancellationConfigPort cancellationConfigRepository,
                                 CancellationConfigMapper cancellationMapper) {
        this.cancellationConfigRepository = cancellationConfigRepository;
        this.cancellationMapper = cancellationMapper;
    }

    @Transactional(readOnly = true)
    public CancellationConfigResponse get() {
        return cancellationMapper.toResponse(findOrCreate());
    }

    @Transactional
    public CancellationConfigResponse update(UpdateCancellationConfigRequest request) {
        CancellationConfig config = findOrCreate();

        if (request.getCancellationWindowHours() != null) config.setCancellationWindowHours(request.getCancellationWindowHours());
        if (request.getFeeType() != null) config.setFeeType(request.getFeeType());
        if (request.getFeeValue() != null) config.setFeeValue(request.getFeeValue());
        if (request.getAfterWindowAllowed() != null) config.setAfterWindowAllowed(request.getAfterWindowAllowed());

        return cancellationMapper.toResponse(cancellationConfigRepository.save(config));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private CancellationConfig findOrCreate() {
        Tenant tenant = requireTenant();
        return cancellationConfigRepository.findByTenant(tenant)
                .orElseGet(() -> cancellationConfigRepository.save(
                        CancellationConfig.builder().tenant(tenant).build()));
    }

}
