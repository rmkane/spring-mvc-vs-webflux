# Acme Auth Service (LDAP)

LDAP-based authentication service providing user lookup and role management.

## Purpose

This module implements an authentication service that stores users and roles in an OpenLDAP directory. It provides a REST API for user information lookup by Distinguished Name (DN).

## Key Features

- User lookup by DN (case-insensitive with exact match fallback)
- Role-based access control with standardized role names (`ACME_READ_WRITE`, `ACME_READ_ONLY`)
- OpenLDAP directory integration using Spring LDAP
- LDIF bootstrap for initial user and role data
- Group-based role assignment (users belong to groups that map to roles)
- RESTful API endpoint: `GET /api/v1/users/{dn}`
- SSL/TLS support with mTLS

## Port

Runs on **port 8082** (HTTPS with mTLS).

## LDAP Structure

Uses OpenLDAP (port 389) with the following structure:

- Base DN: `dc=corp,dc=acme,dc=org`
- Users: `ou=users,ou=<department>,dc=corp,dc=acme,dc=org`
- Groups: `cn=ACME_<role>,ou=groups,dc=corp,dc=acme,dc=org`

Users are assigned roles through group membership. The service queries groups directly using `(member=<userDn>)` filters.

## Interchangeability

This service is interchangeable with `acme-auth-service-db`. Both services provide the same REST API contract and can be swapped without changes to the API modules.
