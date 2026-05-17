package com.carbooking.modules.client.domain.port;

import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.CorporateEmployee;
import com.carbooking.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CorporateEmployeePort {
    Optional<CorporateEmployee> findById(UUID id);
    Page<CorporateEmployee> findByCorporateClient(CorporateClient client, Pageable pageable);
    boolean existsByCorporateClientAndEmployeeCode(CorporateClient client, String employeeCode);
    CorporateEmployee save(CorporateEmployee employee);
    Optional<CorporateEmployee> findByUser(User user);
}
