package com.carbooking.repository;

import com.carbooking.common.enums.InvoiceStatus;
import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.Invoice;
import com.carbooking.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Page<Invoice> findByTenant(Tenant tenant, Pageable pageable);
    Page<Invoice> findByCorporateClient(CorporateClient client, Pageable pageable);
    long countByTenantAndStatus(Tenant tenant, InvoiceStatus status);

    boolean existsByCorporateClientAndBillingPeriodStartLessThanEqualAndBillingPeriodEndGreaterThanEqual(
            CorporateClient client, LocalDate end, LocalDate start);
}
