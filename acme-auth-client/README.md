# Acme Auth Client

REST client library for communicating with the authentication service.

## Purpose

This module provides a reusable client library that wraps HTTP calls to the authentication service. It abstracts the details of making REST calls to retrieve user information and roles.

## Key Features

- REST client interface for authentication service
- DTOs for user information (`UserInfo`, `UserInfoResponse`)
- SSL/TLS configuration support (via `acme-security-core`)
- Framework-agnostic (can be used by both MVC and WebFlux applications)

## Usage

Used by `acme-security-core` to fetch user information from the authentication service. The client is configured with the authentication service URL and optional SSL certificates for mTLS communication.

## Dependencies

- Spring Web (for `RestClient` or `WebClient`)
- Spring Security Core (for security context utilities)
