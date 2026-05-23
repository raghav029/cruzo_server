package com.carbooking.repository;

import com.carbooking.entity.ContactEnquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContactEnquiryRepository extends JpaRepository<ContactEnquiry, UUID> {
    Page<ContactEnquiry> findByTenantId(UUID tenantId, Pageable pageable);
}
