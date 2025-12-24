package org.acme.api.util;

import org.acme.security.core.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextUtil {

    public static UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found in SecurityContext");
        }

        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserPrincipal) {
            return (UserPrincipal) principal;
        }
        
        throw new IllegalStateException("Principal is not of type UserPrincipal: " + principal.getClass().getName());
    }
}

