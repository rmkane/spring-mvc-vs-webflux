# Storing Certificate Issuer DN in LDAP

This document outlines the options for storing X.509 certificate issuer Distinguished Names (DNs) in LDAP.

## Overview

The issuer DN identifies which Certificate Authority (CA) issued a user's certificate. This information is needed for:

- Certificate validation
- Trust chain verification
- User authentication

## Options

### Option 1: Custom LDAP Schema (Recommended for Production)

**Pros:**

- Semantically correct attribute name (`certificateIssuerDN`)
- Type-safe and validated by LDAP server
- Follows LDAP best practices
- Clear intent and documentation

**Cons:**

- Requires schema definition and loading
- More complex setup
- Schema must be loaded before data import

**Implementation:**

1. Define custom schema (see `acme-auth-service-ldap/src/main/resources/ldap/schema/acme-cert.schema`)
2. Load schema into LDAP server
3. Update object classes in LDIF to use `acmeCertificatePerson` or add `certificateIssuerDN` to existing object classes
4. Update Java code to read `certificateIssuerDN` attribute

**Schema File:**

```ldif
attributetype ( 1.3.6.1.4.1.99999.1.1.1
    NAME 'certificateIssuerDN'
    DESC 'X.509 Certificate Issuer Distinguished Name'
    EQUALITY caseIgnoreMatch
    SUBSTR caseIgnoreSubstringsMatch
    SYNTAX 1.3.6.1.4.1.1466.115.121.1.15
    SINGLE-VALUE )
```

### Option 2: Standard LDAP Attributes (Simpler, Current Approach)

**Pros:**

- No schema changes needed
- Works immediately
- Simple to implement

**Cons:**

- Semantically incorrect (using `description` for issuer DN)
- Can conflict with actual description usage
- Less clear intent

**Current Implementation:**

- Using `description` attribute to store issuer DN
- Works but not ideal for production

**Alternative Standard Attributes:**

- `description` - Current choice, but semantically wrong
- `labeledURI` - Could work, but also semantically incorrect
- `userCertificate` - For binary cert data, not issuer DN string

### Option 3: Groups (NOT Recommended)

**Why Not:**

- Issuer DN is a **per-user attribute**, not a group membership
- Users with the same issuer don't form a "group" in the traditional sense
- Would require maintaining separate groups for each issuer
- Doesn't scale well if you have many issuers
- Groups are for roles/permissions, not certificate metadata

## Recommendation

**For Development/Testing:** Use Option 2 (standard attributes like `description`) - it's simple and works.

**For Production:** Use Option 1 (custom schema) - it's the proper LDAP way and provides better semantics and validation.

## Current Status

The codebase currently uses **Option 2** (`description` attribute) as a workaround. To upgrade to **Option 1**:

1. Load the custom schema into LDAP
2. Update LDIF files to use `certificateIssuerDN` instead of `description`
3. Update `UserContextMapper.java` to read `certificateIssuerDN` instead of `description`
4. Reload user data

## Migration Path

If you want to migrate from `description` to `certificateIssuerDN`:

1. Load the custom schema
2. Update existing entries using `ldapmodify`:

   ```bash
   dn: cn=jdoe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org
   changetype: modify
   delete: description
   -
   add: certificateIssuerDN
   certificateIssuerDN: CN=Acme Intermediate CA,O=Acme Corp,C=US
   ```

3. Update LDIF files and Java code
4. Test thoroughly
