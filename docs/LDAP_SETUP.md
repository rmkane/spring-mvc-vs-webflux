# LDAP Setup with Custom Schema

This document explains how the LDAP infrastructure is automatically configured on startup.

## Automatic Setup

When you run `make infra-up` or `docker compose up`, the LDAP container automatically:

1. **Loads Custom Schema** - The `acme-cert.schema` file is automatically loaded, which defines the `certificateIssuerDN` attribute
2. **Loads User Data** - The `01-users.ldif` file is automatically imported, creating all users and role groups
3. **Health Check** - The container waits until users are loaded and the schema is active before marking as healthy

## Schema

The custom schema (`acme-auth-service-ldap/src/main/resources/ldap/schema/acme-cert.schema`) defines:

- **Attribute**: `certificateIssuerDN` - Stores the X.509 certificate issuer Distinguished Name
- **Object Class**: `acmeCertificatePerson` - Extends `inetOrgPerson` with certificate issuer information

## Directory Structure

```none
acme-auth-service-ldap/src/main/resources/ldap/
├── schema/
│   └── acme-cert.schema          # Custom schema definition
└── ldif/
    └── 01-users.ldif             # User and role data
```

## Docker Compose Configuration

The `docker-compose.yml` mounts both directories and the entrypoint script:

1. Copies schema files to `/container/service/slapd/assets/config/bootstrap/schema/custom/`
2. Copies LDIF files to `/container/service/slapd/assets/config/bootstrap/ldif/custom/`
3. The `osixia/openldap` image automatically loads these on startup

## Health Check

The health check verifies that:

- LDAP is running
- The custom schema is loaded (by checking for `certificateIssuerDN` attribute)
- Users are loaded (by querying for `jdoe` user)

## Users

All users are automatically created with:

- Subject DN (from the entry DN)
- Issuer DN (in `certificateIssuerDN` attribute)
- Roles (via LDAP group membership)

## Troubleshooting

If users are not loading:

1. Check LDAP logs: `docker logs acme-ldap`
2. Verify schema is loaded: `docker exec acme-ldap ldapsearch -x -H ldap://localhost:389 -D 'cn=admin,dc=corp,dc=acme,dc=org' -w admin -b 'cn=schema,cn=config' '(objectClass=*)' | grep certificateIssuerDN`
3. Verify users exist: `docker exec acme-ldap ldapsearch -x -H ldap://localhost:389 -D 'cn=admin,dc=corp,dc=acme,dc=org' -w admin -b 'ou=users,dc=corp,dc=acme,dc=org' '(objectClass=inetOrgPerson)' dn`
