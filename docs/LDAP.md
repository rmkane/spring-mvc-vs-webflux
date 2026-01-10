<!-- omit in toc -->
# LDAP Directory Guide

This document explains how to query and interact with the OpenLDAP directory used by the authentication service.

<!-- omit in toc -->
## Table of Contents

- [Overview](#overview)
- [Connection Details](#connection-details)
- [Accessing the LDAP Container](#accessing-the-ldap-container)
  - [Using Make Command](#using-make-command)
  - [Using Docker Exec](#using-docker-exec)
- [Directory Structure](#directory-structure)
  - [Organizational Units](#organizational-units)
  - [User Attributes](#user-attributes)
  - [Group Attributes](#group-attributes)
- [Querying Users](#querying-users)
  - [List All Users](#list-all-users)
  - [Find User by DN](#find-user-by-dn)
  - [Find User by Common Name (CN)](#find-user-by-common-name-cn)
  - [Find User by UID](#find-user-by-uid)
  - [Find Users in a Specific Department](#find-users-in-a-specific-department)
  - [Get User with All Attributes](#get-user-with-all-attributes)
- [Querying Groups (Roles)](#querying-groups-roles)
  - [List All Groups](#list-all-groups)
  - [Find Specific Group](#find-specific-group)
  - [Find Groups Containing a Specific User](#find-groups-containing-a-specific-user)
  - [Get All Members of a Group](#get-all-members-of-a-group)
- [Querying from Outside the Container](#querying-from-outside-the-container)
- [Common Operations](#common-operations)
  - [Search for All Entries](#search-for-all-entries)
  - [Search with Specific Attributes](#search-with-specific-attributes)
  - [Case-Insensitive Search](#case-insensitive-search)
- [Example Queries](#example-queries)
  - [Get User Information (as returned by auth service)](#get-user-information-as-returned-by-auth-service)
  - [Find All Users with READ\_WRITE Role](#find-all-users-with-read_write-role)
  - [Verify User's Roles](#verify-users-roles)
- [LDAP Search Filter Syntax](#ldap-search-filter-syntax)
  - [Examples](#examples)
- [Troubleshooting](#troubleshooting)
  - [Container Not Running](#container-not-running)
  - [Connection Refused](#connection-refused)
  - [Authentication Failed](#authentication-failed)
  - [No Results Returned](#no-results-returned)
- [Additional Resources](#additional-resources)

## Overview

The project uses an OpenLDAP container (`acme-ldap`) to store users and groups (roles). The LDAP directory is automatically populated with sample data on startup from LDIF files.

## Connection Details

- **Container Name**: `acme-ldap`
- **LDAP Port**: `389` (mapped to host)
- **LDAPS Port**: `636` (mapped to host, TLS disabled by default)
- **Base DN**: `dc=corp,dc=acme,dc=org`
- **Admin DN**: `cn=admin,dc=corp,dc=acme,dc=org`
- **Admin Password**: `admin`

## Accessing the LDAP Container

### Using Make Command

The easiest way to access the LDAP container is using the make command:

```bash
make ldap-exec
```

This opens an interactive bash shell inside the container.

### Using Docker Exec

You can also use `docker exec` directly:

```bash
docker exec -it acme-ldap bash
```

## Directory Structure

The LDAP directory follows this structure:

```text
dc=corp,dc=acme,dc=org
├── ou=users
│   ├── ou=engineering
│   │   └── cn=john doe
│   ├── ou=hr
│   │   └── cn=alice smith
│   ├── ou=finance
│   │   └── cn=brian wilson
│   ├── ou=it
│   │   └── cn=maria garcia
│   └── ou=security
│       └── cn=kevin tran
└── ou=roles
    ├── cn=ACME_READ_WRITE
    └── cn=ACME_READ_ONLY
```

### Organizational Units

- **Users**: `ou=users,dc=corp,dc=acme,dc=org`
  - Users are organized by department (engineering, hr, finance, it, security)
  - User DN format: `cn=<name>,ou=<department>,ou=users,dc=corp,dc=acme,dc=org`
  
- **Roles**: `ou=roles,dc=corp,dc=acme,dc=org`
  - Groups representing application roles
  - Group DN format: `cn=ACME_<ROLE>,ou=roles,dc=corp,dc=acme,dc=org`

### User Attributes

Users have the following attributes:

- `cn` (Common Name): Full name (e.g., "John Doe")
- `givenName`: First name
- `sn` (Surname): Last name
- `uid`: User ID (e.g., "jdoe")
- `userPassword`: Hashed password
- `memberOf`: Groups the user belongs to (automatically maintained)

### Group Attributes

Groups (roles) have the following attributes:

- `cn` (Common Name): Role name (e.g., "ACME_READ_WRITE")
- `description`: Role description
- `member`: List of user DNs that belong to this group

## Querying Users

### List All Users

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "ou=users,dc=corp,dc=acme,dc=org" \
  "(objectClass=inetOrgPerson)"
```

### Find User by DN

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "dc=corp,dc=acme,dc=org" \
  "(distinguishedName=cn=john doe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org)"
```

### Find User by Common Name (CN)

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "ou=users,dc=corp,dc=acme,dc=org" \
  "(cn=John Doe)"
```

### Find User by UID

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "ou=users,dc=corp,dc=acme,dc=org" \
  "(uid=jdoe)"
```

### Find Users in a Specific Department

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "ou=engineering,ou=users,dc=corp,dc=acme,dc=org" \
  "(objectClass=inetOrgPerson)"
```

### Get User with All Attributes

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "dc=corp,dc=acme,dc=org" \
  "(cn=John Doe)" \
  "*" "+"
```

## Querying Groups (Roles)

### List All Groups

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "ou=roles,dc=corp,dc=acme,dc=org" \
  "(objectClass=groupOfNames)"
```

### Find Specific Group

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "ou=roles,dc=corp,dc=acme,dc=org" \
  "(cn=ACME_READ_WRITE)"
```

### Find Groups Containing a Specific User

This is the query used by the auth service to determine user roles:

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "dc=corp,dc=acme,dc=org" \
  "(member=cn=john doe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org)"
```

### Get All Members of a Group

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "cn=ACME_READ_WRITE,ou=roles,dc=corp,dc=acme,dc=org" \
  "(objectClass=groupOfNames)" \
  member
```

## Querying from Outside the Container

If you want to query LDAP from your host machine (not inside the container), use:

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "dc=corp,dc=acme,dc=org" \
  "(objectClass=*)"
```

Note: You need `ldapsearch` installed on your host. On macOS, install with:

```bash
brew install openldap
```

## Common Operations

### Search for All Entries

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "dc=corp,dc=acme,dc=org" \
  "(objectClass=*)"
```

### Search with Specific Attributes

To return only specific attributes:

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "ou=users,dc=corp,dc=acme,dc=org" \
  "(objectClass=inetOrgPerson)" \
  cn givenName sn uid
```

### Case-Insensitive Search

LDAP searches are case-insensitive by default. Both of these work:

```bash
# Both return the same result
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "ou=users,dc=corp,dc=acme,dc=org" \
  "(cn=John Doe)"

ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "ou=users,dc=corp,dc=acme,dc=org" \
  "(cn=john doe)"
```

## Example Queries

### Get User Information (as returned by auth service)

```bash
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "dc=corp,dc=acme,dc=org" \
  "(cn=John Doe)" \
  dn givenName sn memberOf
```

### Find All Users with READ_WRITE Role

```bash
# First, get the group DN
GROUP_DN="cn=ACME_READ_WRITE,ou=roles,dc=corp,dc=acme,dc=org"

# Then find all members
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "$GROUP_DN" \
  "(objectClass=groupOfNames)" \
  member
```

### Verify User's Roles

```bash
USER_DN="cn=john doe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org"

ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "dc=corp,dc=acme,dc=org" \
  "(member=$USER_DN)" \
  cn description
```

## LDAP Search Filter Syntax

Common filter operators:

- `(attribute=value)` - Equality match
- `(attribute=*value*)` - Substring match (contains)
- `(attribute=value*)` - Starts with
- `(*attribute=value)` - Ends with
- `(&(filter1)(filter2))` - AND (both conditions must match)
- `(|(filter1)(filter2))` - OR (either condition matches)
- `(!(filter))` - NOT (negation)

### Examples

```bash
# Find users with "John" in their name
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "ou=users,dc=corp,dc=acme,dc=org" \
  "(cn=*John*)"

# Find users in Engineering OR HR
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "ou=users,dc=corp,dc=acme,dc=org" \
  "(|(ou=engineering)(ou=hr))"

# Find groups that are NOT ACME_READ_ONLY
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "ou=roles,dc=corp,dc=acme,dc=org" \
  "(&(objectClass=groupOfNames)(!(cn=ACME_READ_ONLY)))"
```

## Troubleshooting

### Container Not Running

If the container is not running, start it:

```bash
make ldap-up
# or
docker compose up -d ldap
```

### Connection Refused

Ensure the LDAP container is healthy:

```bash
docker ps | grep acme-ldap
docker logs acme-ldap
```

### Authentication Failed

Verify you're using the correct admin credentials:

- DN: `cn=admin,dc=corp,dc=acme,dc=org`
- Password: `admin`

### No Results Returned

- Check that the base DN is correct: `dc=corp,dc=acme,dc=org`
- Verify the search filter syntax
- Ensure the container has finished initializing (check logs)

## Additional Resources

- [OpenLDAP Documentation](https://www.openldap.org/doc/)
- [LDAP Search Filters](https://ldap.com/ldap-filters/)
- [LDAP DN Syntax](https://ldap.com/ldap-dns-and-rdns/)
