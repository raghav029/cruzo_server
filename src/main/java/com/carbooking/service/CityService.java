package com.carbooking.service;

import com.carbooking.common.exception.DuplicateResourceException;
import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.util.SecurityUtils;
import com.carbooking.dto.request.city.CreateCityRequest;
import com.carbooking.dto.request.city.UpdateCityRequest;
import com.carbooking.dto.response.city.CityResponse;
import com.carbooking.entity.ServiceCity;
import com.carbooking.entity.Tenant;
import com.carbooking.repository.ServiceCityRepository;
import com.carbooking.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CityService {

    private final ServiceCityRepository cityRepo;
    private final TenantRepository tenantRepo;

    @Transactional
    public CityResponse create(CreateCityRequest req) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepo.findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        if (cityRepo.existsByTenantAndNameIgnoreCase(tenant, req.getName()))
            throw new DuplicateResourceException("City '" + req.getName() + "' already exists");

        ServiceCity city = ServiceCity.builder()
            .tenant(tenant)
            .name(req.getName().trim())
            .active(true)
            .build();

        return toResponse(cityRepo.save(city));
    }

    public List<CityResponse> listAll() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepo.findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        return cityRepo.findByTenant(tenant).stream()
            .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public CityResponse update(UUID cityId, UpdateCityRequest req) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepo.findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        ServiceCity city = cityRepo.findByIdAndTenant(cityId, tenant)
            .orElseThrow(() -> new ResourceNotFoundException("City not found"));

        if (req.getName() != null && !req.getName().isBlank()) {
            String newName = req.getName().trim();
            if (!newName.equalsIgnoreCase(city.getName()) &&
                cityRepo.existsByTenantAndNameIgnoreCase(tenant, newName))
                throw new DuplicateResourceException("City '" + newName + "' already exists");
            city.setName(newName);
        }

        if (req.getActive() != null) {
            city.setActive(req.getActive());
        }

        return toResponse(cityRepo.save(city));
    }

    @Transactional
    public void deactivate(UUID cityId) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        Tenant tenant = tenantRepo.findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        ServiceCity city = cityRepo.findByIdAndTenant(cityId, tenant)
            .orElseThrow(() -> new ResourceNotFoundException("City not found"));

        city.setActive(false);
        cityRepo.save(city);
    }

    public List<CityResponse> listActivePublic(UUID tenantId) {
        return cityRepo.findByTenantIdAndActiveTrue(tenantId).stream()
            .map(this::toResponse).collect(Collectors.toList());
    }

    private CityResponse toResponse(ServiceCity c) {
        return CityResponse.builder()
            .id(c.getId())
            .name(c.getName())
            .active(c.isActive())
            .createdAt(c.getCreatedAt())
            .build();
    }
}
