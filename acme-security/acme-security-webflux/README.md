# Acme Security WebFlux

WebFlux-specific security configuration for Spring WebFlux applications.

## Purpose

This module provides Spring Security configuration specifically for Spring WebFlux (reactive stack) applications. It integrates the core security logic with the WebFlux framework.

## Key Features

- `SecurityConfig` - Spring Security WebFlux configuration
- `AuthenticationWebFilter` - Reactive filter for header-based authentication
- Integration with `acme-security-core` for authentication logic
- Public endpoint exclusion
- Role-based access control
- Reactive streams support

## Usage

Used by `acme-api-webflux` to secure the REST API endpoints. The filter intercepts requests, extracts the `x-dn` header, and authenticates the user via the core security services using reactive types.

## Dependencies

- `acme-security-core` - Core security logic
- Spring Security WebFlux (reactive stack)
