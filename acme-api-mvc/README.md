# Acme API MVC

Spring MVC-based REST API implementation using the traditional blocking servlet stack.

## Purpose

This module provides a REST API for book management using Spring MVC, demonstrating the traditional blocking I/O approach. It uses:

- **Spring MVC** for HTTP request handling
- **Servlet stack** (Tomcat embedded server)
- **JPA/Hibernate** for database persistence (via `acme-persistence-jpa`)
- **Blocking I/O** throughout the request/response cycle

## Key Features

- CRUD operations for book resources
- Header-based authentication via `x-dn` header
- Role-based access control (RBAC)
- Integration with authentication service for user lookup
- OpenAPI/Swagger documentation

## Port

Runs on **port 8080** (HTTPS with mTLS).

## Dependencies

- `acme-security-webmvc` - MVC-specific security configuration
- `acme-persistence-jpa` - JPA entities and repositories
