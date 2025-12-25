package org.acme.api.util;

import org.acme.security.core.model.UserInformation;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextUtil {

    public static UserInformation getCurrentUserInformation() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found in SecurityContext");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserInformation userInformation) {
            return userInformation;
        }

        throw new IllegalStateException("Principal is not of type UserInformation: " + principal.getClass().getName());
    }
}
