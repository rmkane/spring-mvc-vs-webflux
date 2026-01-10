# Acme Security WebMVC

MVC-specific security configuration for Spring MVC applications.

## Purpose

This module provides Spring Security configuration specifically for Spring MVC (servlet stack) applications. It integrates the core security logic with the MVC framework.

## Key Features

- `SecurityConfig` - Spring Security configuration for MVC
- `AuthenticationFilter` - Servlet filter for header-based authentication
- Integration with `acme-security-core` for authentication logic
- Public endpoint exclusion
- Role-based access control

## Usage

Used by `acme-api-mvc` to secure the REST API endpoints. The filter intercepts requests, extracts the `x-dn` header, and authenticates the user via the core security services.

## Dependencies

- `acme-security-core` - Core security logic
- Spring Security Web (servlet stack)
