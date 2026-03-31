# Acme Security Core

Core security logic shared between MVC and WebFlux applications.

## Purpose

This module contains paradigm-agnostic security components that can be used by both Spring MVC and Spring WebFlux applications. It provides the core authentication and authorization logic.

## Key Features

- `AuthenticationService` - Handles authentication and creates Spring Security `Authentication` objects
- `CachedUserLookupService` - Caches user lookups to reduce calls to the authentication service
- `UserInformation` - Model for user information and roles
- `SecurityConstants` - Security-related constants (headers, messages, public endpoints)
- `DnUtil` - DN validation and normalization utilities
- `PathMatcherUtil` - Path matching for public endpoints
- `SslConfig` - SSL/TLS configuration for auth service client

## Dependencies

- `acme-auth-client` - For communicating with the authentication service
- Spring Security Core
- Caffeine cache for user lookup caching

## Usage

This module is used by both `acme-security-webmvc` and `acme-security-webflux` to provide shared security functionality.
