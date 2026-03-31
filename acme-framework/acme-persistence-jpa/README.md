# Acme Persistence JPA

JPA-based persistence layer for blocking database operations.

## Purpose

This module provides the persistence layer for the MVC API using Spring Data JPA and Hibernate. It implements blocking, synchronous database operations suitable for the servlet stack.

## Key Features

- JPA entities (`Book`)
- Spring Data JPA repositories (`BookRepository`)
- Flyway database migrations
- PostgreSQL database support
- Blocking I/O operations

## Database

Uses a dedicated PostgreSQL database (port 5432) for application data. The schema includes:

- `books` table - stores book information with audit fields (created_at, created_by, updated_at, updated_by)

## Usage

Used by `acme-api-mvc` for all database operations. All repository methods are blocking and return synchronous results.
