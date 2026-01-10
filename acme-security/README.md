# Acme Security

Shared security infrastructure for authentication and authorization.

## Purpose

This module provides a multi-module security layer that implements header-based authentication and role-based access control. It is designed to work with both Spring MVC and Spring WebFlux applications.

## Module Structure

- **`acme-security-core`** - Core security logic (paradigm-agnostic)
- **`acme-security-webmvc`** - MVC-specific security configuration
- **`acme-security-webflux`** - WebFlux-specific security configuration

## Key Features

- Header-based authentication via `x-dn` header
- User lookup caching (Caffeine cache)
- Role-based access control (RBAC)
- SSL/TLS configuration for auth service communication
- DN validation and normalization
- Public endpoint exclusion (actuator, Swagger, etc.)

## Usage

- MVC applications depend on `acme-security-webmvc`
- WebFlux applications depend on `acme-security-webflux`
- Both depend on `acme-security-core` for shared logic
