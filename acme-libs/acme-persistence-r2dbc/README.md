# Acme Persistence R2DBC

R2DBC-based persistence layer for reactive database operations.

## Purpose

This module provides the persistence layer for the WebFlux API using Spring Data R2DBC. It implements reactive, non-blocking database operations suitable for the reactive stack.

## Key Features

- R2DBC entities (`Book`)
- Spring Data R2DBC repositories (`BookRepository`)
- Flyway database migrations
- PostgreSQL database support
- Reactive streams with `Mono` and `Flux` return types
- Non-blocking I/O operations

## Database

Uses a dedicated PostgreSQL database (port 5433) for application data. The schema includes:

- `books` table - stores book information with audit fields (created_at, created_by, updated_at, updated_by)

## Usage

Used by `acme-api-webflux` for all database operations. All repository methods return reactive types (`Mono<T>`, `Flux<T>`) and are non-blocking.
