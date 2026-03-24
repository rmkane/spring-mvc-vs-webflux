# Acme Security

Shared security infrastructure for authentication and authorization.

## Purpose

This module provides a multi-module security layer that implements header-based authentication and role-based access control. It is designed to work with both Spring MVC and Spring WebFlux applications.

## Module Structure

- **`acme-security-core`** - Core security logic (paradigm-agnostic)
- **`acme-security-webmvc`** - MVC-specific security configuration
- **`acme-security-webflux`** - WebFlux-specific security configuration

## Key Features

- Header-based authentication via configurable subject and issuer DN headers (default: `x-amzn-mtls-clientcert-subject`, `x-amzn-mtls-clientcert-issuer`; override with `acme.security.headers.subject-dn` / `issuer-dn`)
- User lookup caching (Caffeine cache)
- Role-based access control (RBAC)
- SSL/TLS configuration for auth service communication
- DN validation and normalization
- Public endpoint exclusion (actuator, Swagger, etc.)

## Usage

- MVC applications depend on `acme-security-webmvc`
- WebFlux applications depend on `acme-security-webflux`
- Both depend on `acme-security-core` for shared logic

## Configuration

On protected requests, the **pre-authentication** principal is `HeaderCertificatePrincipal` (subject + issuer from headers), built in `HeaderCertificatePreAuthenticatedProcessingFilter` (MVC) and in the WebFlux `ServerAuthenticationConverter`. After a successful lookup, the authenticated principal is still `UserInformation`. If the auth service returns an issuer DN for the user, it must match the request issuer (after `DnUtil` normalization).

For a Spring configuration reference (header names, header logging, etc.), see `acme-security-core/src/test/resources/sample-application.yml`.

For architecture, policy semantics, filter ordering, and a **porting checklist** for other projects, see **[FILTER.md](FILTER.md)**.

### Properties (records in `acme-security-core`)

| Prefix | Record | Purpose |
|--------|--------|---------|
| `acme.security.headers` | `HeadersProperties` | Subject/issuer header names (binds `subject-dn`, `issuer-dn` from YAML). Used by MVC/WebFlux security, DN validation, and request/response header logging (values redacted as `***`). |
| `acme.security.header-filter` | `HeaderFilterProperties` | Optional DEBUG header logging: `disabled`, JSON `ignore-headers` (e.g. `user-agent` patterns such as `kube-probe/*`, `HealthChecker/*`, `ELB-HealthChecker/*` — see `scripts/simulator/simulate-traffic.sh`). Suppression is header-driven only; when rules match, attribute `AcmeHeaderLoggingAttributes.ATTRIBUTE_NAME` (`acme.security.header-filter.suppressed`) is set. Use `AcmeHeaderLoggingRequestAttributes` / `AcmeHeaderLoggingExchangeAttributes` to put, clear, or read it. |

`@EnableConfigurationProperties` is registered on `AcmeSecurityPropertiesConfiguration`.

### Startup listeners

- **MVC:** `WebMvcSecurityStartupListener` — logs a policy snapshot at `ApplicationReadyEvent`.
- **WebFlux:** `WebFluxSecurityStartupListener` — same for reactive apps.

Shared logic: `AbstractAcmeSecurityStartupListener` in core.
