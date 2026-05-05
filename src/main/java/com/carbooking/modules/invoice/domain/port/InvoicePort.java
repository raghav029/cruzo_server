package com.carbooking.modules.invoice.domain.port;

import com.carbooking.common.enums.InvoiceStatus;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.Invoice;
import com.carbooking.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface InvoicePort {
    Optional<Invoice> findById(UUID id);
    Page<Invoice> findByTenant(Tenant tenant, Pageable pageable);
    Page<Invoice> findByCorporateClient(CorporateClient client, Pageable pageable);
    boolean existsByCorporateClientAndBillingPeriodStartLessThanEqualAndBillingPeriodEndGreaterThanEqual(CorporateClient client, LocalDate start, LocalDate end);
    long countByTenantAndStatus(Tenant tenant, InvoiceStatus status);
    Invoice save(Invoice invoice);
}
