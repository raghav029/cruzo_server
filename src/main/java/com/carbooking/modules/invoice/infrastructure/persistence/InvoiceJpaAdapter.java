package com.carbooking.modules.invoice.infrastructure.persistence;

import com.carbooking.common.enums.InvoiceStatus;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.Invoice;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.invoice.domain.port.InvoicePort;
import com.carbooking.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InvoiceJpaAdapter implements InvoicePort {

    private final InvoiceRepository repo;

    @Override public Optional<Invoice> findById(UUID id) { return repo.findById(id); }
    @Override public Page<Invoice> findByTenant(Tenant tenant, Pageable pageable) { return repo.findByTenant(tenant, pageable); }
    @Override public Page<Invoice> findByCorporateClient(CorporateClient client, Pageable pageable) { return repo.findByCorporateClient(client, pageable); }
    @Override public boolean existsByCorporateClientAndBillingPeriodStartLessThanEqualAndBillingPeriodEndGreaterThanEqual(CorporateClient client, LocalDate start, LocalDate end) { return repo.existsByCorporateClientAndBillingPeriodStartLessThanEqualAndBillingPeriodEndGreaterThanEqual(client, start, end); }
    @Override public long countByTenantAndStatus(Tenant tenant, InvoiceStatus status) { return repo.countByTenantAndStatus(tenant, status); }
    @Override public Invoice save(Invoice invoice) { return repo.save(invoice); }
}
