package com.carbooking.repository;

import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.DailySchedule;
import com.carbooking.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DailyScheduleRepository extends JpaRepository<DailySchedule, UUID> {
    Page<DailySchedule> findByTenant(Tenant tenant, Pageable pageable);
    Page<DailySchedule> findByTenantAndCorporateClient(Tenant tenant, CorporateClient client, Pageable pageable);
    List<DailySchedule> findByIsActiveTrue();
}
