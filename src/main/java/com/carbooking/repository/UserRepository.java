package com.carbooking.repository;

import com.carbooking.common.enums.Role;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndTenant(String email, Tenant tenant);
    long countByTenantAndRole(Tenant tenant, com.carbooking.common.enums.Role role);
    List<User> findByTenantAndRole(Tenant tenant, Role role);
    Optional<User> findByEmailAndTenant(String email, Tenant tenant);
    Optional<User> findByRefreshToken(String refreshToken);
}
