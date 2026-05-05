package com.carbooking.modules.dailyschedule.domain.port;

import com.carbooking.entity.DailySchedule;
import com.carbooking.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface DailySchedulePort {
    Optional<DailySchedule> findById(UUID id);
    Page<DailySchedule> findByTenant(Tenant tenant, Pageable pageable);
    DailySchedule save(DailySchedule schedule);
}
