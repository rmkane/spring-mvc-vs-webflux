# Acme API WebFlux

Spring WebFlux-based REST API implementation using the reactive non-blocking stack.

## Purpose

This module provides a REST API for book management using Spring WebFlux, demonstrating the modern reactive non-blocking I/O approach. It uses:

- **Spring WebFlux** for HTTP request handling
- **Netty** embedded server
- **R2DBC** for reactive database persistence (via `acme-persistence-r2dbc`)
- **Non-blocking I/O** throughout the request/response cycle

## Key Features

- CRUD operations for book resources
- Header-based authentication via `x-dn` header
- Role-based access control (RBAC)
- Integration with authentication service for user lookup
- OpenAPI/Swagger documentation
- Reactive streams and backpressure support

## Port

Runs on **port 8081** (HTTPS with mTLS).

## Dependencies

- `acme-security-webflux` - WebFlux-specific security configuration
- `acme-persistence-r2dbc` - R2DBC entities and repositories
