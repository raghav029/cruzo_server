package com.carbooking.modules.dailyschedule.infrastructure.persistence;

import com.carbooking.entity.DailySchedule;
import com.carbooking.entity.Tenant;
import com.carbooking.modules.dailyschedule.domain.port.DailySchedulePort;
import com.carbooking.repository.DailyScheduleRepository;
import com.carbooking.modules.dailyschedule.domain.port.DailySchedulePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DailyScheduleJpaAdapter implements DailySchedulePort {

    private final DailyScheduleRepository repo;

    @Override public Optional<DailySchedule> findById(UUID id) { return repo.findById(id); }
    @Override public Page<DailySchedule> findByTenant(Tenant tenant, Pageable pageable) { return repo.findByTenant(tenant, pageable); }
    @Override public DailySchedule save(DailySchedule schedule) { return repo.save(schedule); }
}
