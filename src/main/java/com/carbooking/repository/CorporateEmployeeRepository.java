package com.carbooking.repository;

import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.CorporateEmployee;
import com.carbooking.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CorporateEmployeeRepository extends JpaRepository<CorporateEmployee, UUID> {
    Page<CorporateEmployee> findByCorporateClient(CorporateClient client, Pageable pageable);
    Optional<CorporateEmployee> findByUser(User user);
    boolean existsByCorporateClientAndEmployeeCode(CorporateClient client, String employeeCode);
}
