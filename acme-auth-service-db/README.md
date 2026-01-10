# Acme Auth Service (Database)

PostgreSQL-based authentication service providing user lookup and role management.

## Purpose

This module implements an authentication service that stores users and roles in a PostgreSQL database. It provides a REST API for user information lookup by Distinguished Name (DN).

## Key Features

- User lookup by DN (case-insensitive)
- Role-based access control with standardized role names (`ACME_READ_WRITE`, `ACME_READ_ONLY`)
- PostgreSQL database storage using Spring Data JPA
- Flyway database migrations for schema and seed data
- RESTful API endpoint: `GET /api/v1/users/{dn}`
- SSL/TLS support with mTLS

## Port

Runs on **port 8082** (HTTPS with mTLS).

## Database

Uses a dedicated PostgreSQL database (port 5434) for authentication data. The schema includes:

- `users` table - stores user information with DN
- `roles` table - stores available roles
- `user_roles` table - many-to-many relationship between users and roles

## Interchangeability

This service is interchangeable with `acme-auth-service-ldap`. Both services provide the same REST API contract and can be swapped without changes to the API modules.
