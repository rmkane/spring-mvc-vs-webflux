# SSL Certificates

This directory contains keystores and truststores for the Auth Service (LDAP variant).

## Generating Certificates

**These files are NOT committed to version control.** To generate them, run:

```bash
./acme-infrastructure/scripts/certs/setup-all-certs.sh
```

This script will:

1. Generate Root CA and Intermediate CA
2. Generate server certificates for all services
3. Generate user certificates for browser authentication
4. Create keystores and truststores
5. Copy them to this directory (and other service directories)

## Files

- `auth-service-keystore.jks` - Server keystore containing the service certificate and private key
- `acme-truststore.jks` - Truststore containing the CA chain for validating client certificates

## Password

Default password for all keystores/truststores: `changeit`

**Note:** Change this password in production environments.
