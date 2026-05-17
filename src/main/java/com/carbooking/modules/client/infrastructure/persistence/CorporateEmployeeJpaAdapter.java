package com.carbooking.modules.client.infrastructure.persistence;

import com.carbooking.entity.CorporateClient;
import com.carbooking.entity.CorporateEmployee;
import com.carbooking.entity.User;
import com.carbooking.modules.client.domain.port.CorporateEmployeePort;
import com.carbooking.repository.CorporateEmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CorporateEmployeeJpaAdapter implements CorporateEmployeePort {

    private final CorporateEmployeeRepository repo;

    @Override public Optional<CorporateEmployee> findById(UUID id) { return repo.findById(id); }
    @Override public Page<CorporateEmployee> findByCorporateClient(CorporateClient client, Pageable pageable) { return repo.findByCorporateClient(client, pageable); }
    @Override public boolean existsByCorporateClientAndEmployeeCode(CorporateClient client, String code) { return repo.existsByCorporateClientAndEmployeeCode(client, code); }
    @Override public CorporateEmployee save(CorporateEmployee employee) { return repo.save(employee); }
    @Override public Optional<CorporateEmployee> findByUser(User user) { return repo.findByUser(user); }
}
