package org.acme.security.core.model;

import java.security.Principal;

/**
 * Client identity from mTLS-style subject and issuer DN headers, before auth
 * service lookup. Carries only the two header values; roles and profile fields
 * arrive after {@link org.acme.security.core.service.AuthenticationService}
 * completes.
 */
public record HeaderCertificatePrincipal(String subjectDn, String issuerDn) implements Principal {

    @Override
    public String getName() {
        return subjectDn;
    }
}
