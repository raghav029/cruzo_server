package com.carbooking.common.util;

import com.carbooking.common.exception.ResourceNotFoundException;
import com.carbooking.common.exception.UnauthorizedException;
import com.carbooking.entity.Tenant;
import com.carbooking.entity.User;
import com.carbooking.repository.TenantRepository;
import com.carbooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Base class for all application-layer services that touch tenant-scoped data.
 *
 * Repositories are injected via @Autowired so subclasses can use @RequiredArgsConstructor
 * with only their own dependencies — no super() boilerplate needed.
 *
 * Services that inject @Lazy beans (e.g. NotificationService) still need an explicit
 * constructor, but they no longer include tenantRepository/userRepository params.
 */
public abstract class TenantSupport {

    @Autowired
    protected TenantRepository tenantRepository;

    @Autowired
    protected UserRepository userRepository;

    /** Resolves the current tenant from the JWT. Throws 404 if the record does not exist. */
    protected Tenant requireTenant() {
        UUID id = SecurityUtils.getCurrentTenantId();
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    /**
     * Guards against cross-tenant access. SUPER_ADMIN (tenantId = null in JWT) bypasses automatically.
     * Throws 403 if the entity belongs to a different tenant.
     */
    protected void assertSameTenant(UUID entityTenantId) {
        UUID currentTenantId = SecurityUtils.getCurrentTenantId();
        if (currentTenantId == null) return;
        if (!entityTenantId.equals(currentTenantId)) {
            throw new UnauthorizedException("Access denied");
        }
    }

    /**
     * Loads an entity by its Optional result, asserts tenant ownership, and returns it.
     * Replaces the repetitive findById + orElseThrow + assertSameTenant pattern.
     *
     * Usage:
     *   Vehicle v = findOrFail(vehicleRepository.findById(id), "Vehicle", v -> v.getTenant().getId());
     */
    protected <T> T findOrFail(Optional<T> result, String entityName, Function<T, UUID> tenantIdExtractor) {
        T entity = result.orElseThrow(() -> new ResourceNotFoundException(entityName + " not found"));
        assertSameTenant(tenantIdExtractor.apply(entity));
        return entity;
    }

    /** Returns the tenantId from the JWT without loading the Tenant entity. */
    protected UUID currentTenantId() {
        return SecurityUtils.getCurrentTenantId();
    }

    /** Returns the userId from the JWT without loading the User entity. */
    protected UUID currentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    /** Returns the currently authenticated User entity. */
    protected User currentUser() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /** Returns the Spring Security role string (e.g. "ROLE_FLEET_MANAGER"). */
    protected String currentRole() {
        return SecurityUtils.getCurrentRole();
    }
}
