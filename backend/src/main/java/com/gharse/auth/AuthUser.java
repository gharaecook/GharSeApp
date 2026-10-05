package com.gharse.auth;

import com.gharse.common.ApiException;
import com.gharse.user.UserRole;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** The authenticated principal placed in the security context from a validated JWT. */
public record AuthUser(UUID id, String email, UserRole role) {

    public static AuthUser current() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof AuthUser u) return u;
        throw ApiException.unauthorized("Authentication required");
    }

    /** The caller, or null for anonymous requests on public endpoints. */
    public static AuthUser currentOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getPrincipal() instanceof AuthUser u ? u : null;
    }
}
