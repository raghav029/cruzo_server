package com.carbooking.service;

import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.dto.request.public_.ContactEnquiryRequest;
import com.carbooking.entity.ContactEnquiry;
import com.carbooking.entity.Tenant;
import com.carbooking.repository.ContactEnquiryRepository;
import com.carbooking.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactEnquiryService {

    private final ContactEnquiryRepository enquiryRepo;
    private final TenantRepository tenantRepo;

    @Transactional
    public void submit(ContactEnquiryRequest req) {
        Tenant tenant = tenantRepo.findById(req.getTenantId())
            .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        enquiryRepo.save(ContactEnquiry.builder()
            .tenant(tenant).name(req.getName())
            .email(req.getEmail()).phone(req.getPhone())
            .message(req.getMessage()).build());
    }
}
