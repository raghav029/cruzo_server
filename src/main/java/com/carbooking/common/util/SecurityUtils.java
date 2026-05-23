package com.carbooking.common.util;

import com.carbooking.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UserPrincipal) auth.getPrincipal();
    }

    public static UUID getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public static UUID getCurrentTenantId() {
        return getCurrentUser().getTenantId();
    }

    public static String getCurrentRole() {
        return getCurrentUser().getRole();
    }

    public static String getCurrentBookingMode() {
        return getCurrentUser().getBookingMode();
    }

    public static UUID getCurrentCustomerId() {
        return getCurrentUser().getUserId();
    }
}
