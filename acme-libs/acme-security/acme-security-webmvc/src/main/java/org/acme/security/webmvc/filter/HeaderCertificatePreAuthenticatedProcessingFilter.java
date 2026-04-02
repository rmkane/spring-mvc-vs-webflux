package org.acme.security.webmvc.filter;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

import org.acme.security.core.config.properties.HeadersProperties;
import org.acme.security.core.model.HeaderCertificatePrincipal;
import org.acme.security.core.util.PathMatcherUtil;

/**
 * Builds a {@link HeaderCertificatePrincipal} from configured subject/issuer
 * headers as early as the pre-auth filter runs (after
 * {@link DnValidationFilter} on protected paths).
 */
@Slf4j
public class HeaderCertificatePreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {

    private final HeadersProperties headersProperties;

    public HeaderCertificatePreAuthenticatedProcessingFilter(HeadersProperties headersProperties) {
        this.headersProperties = headersProperties;
        setCheckForPrincipalChanges(false);
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
        if (PathMatcherUtil.isPublicEndpoint(request.getRequestURI())) {
            return null;
        }
        String subject = request.getHeader(headersProperties.subjectDn());
        String issuer = request.getHeader(headersProperties.issuerDn());
        if (!StringUtils.hasText(subject) || !StringUtils.hasText(issuer)) {
            log.warn("Missing subject or issuer header");
            return null;
        }
        return new HeaderCertificatePrincipal(subject.trim(), issuer.trim());
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        return "";
    }
}
