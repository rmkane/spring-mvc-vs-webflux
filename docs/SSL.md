# Mutual TLS (mTLS) Configuration

This document explains the mutual TLS (mTLS) setup used for secure communication between the authentication service and the API clients (MVC and WebFlux).

## Overview

Mutual TLS (mTLS) is a security protocol where both the client and server authenticate each other using X.509 certificates. Unlike standard TLS (one-way TLS), where only the server presents a certificate, mTLS requires both parties to present and verify certificates.

In this project, the **auth service** acts as the HTTPS server, and the **MVC and WebFlux APIs** act as clients that authenticate to the auth service.

## Architecture

```none
┌───────────────┐     mTLS     ┌────────────────┐
│ MVC API       │ <──────────> │ Auth Service   │
│ (Client)      │              │ (Server)       │
│               │              │                │
│ - Client Cert │              │ - Server Cert  │
│ - Truststore  │              │ - Truststore   │
│               │              │ - client-auth: │
│               │              │   need         │
└───────────────┘              └────────────────┘
         │                             ▲
         │         mTLS                │
         └─────────────────────────────┘
                    │
         ┌──────────┴──────────┐
         │                     │
┌─────────────────┐   ┌─────────────────┐
│  WebFlux API    │   │ (Other clients) │
│  (Client)       │   │                 │
│                 │   │                 │
│ - Client Cert   │   │                 │
│ - Truststore    │   │                 │
└─────────────────┘   └─────────────────┘
```

## Certificate Structure

### Keystores

Each service has its own **keystore** containing its identity certificate and private key:

- **`auth-service-keystore.jks`**: Server certificate for the auth service
- **`api-mvc-keystore.jks`**: Client certificate for the MVC API
- **`api-webflux-keystore.jks`**: Client certificate for the WebFlux API

### Truststore

All services share the same **truststore** (`auth-service-truststore.jks`) that contains:

- The auth service's certificate (for clients to verify the server)
- The MVC API's certificate (for the server to verify the MVC client)
- The WebFlux API's certificate (for the server to verify the WebFlux client)

This shared truststore enables mutual authentication: clients trust the server, and the server trusts the clients.

## Component Roles

### Auth Service (Server)

**Location**: `acme-auth-service/src/main/resources/ssl/`

**Configuration** (`application.yml`):

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:ssl/auth-service-keystore.jks
    key-store-password: changeit
    trust-store: classpath:ssl/auth-service-truststore.jks
    trust-store-password: changeit
    client-auth: need  # Requires client certificates
```

**Role**:

- Presents its server certificate to clients (from keystore)
- Verifies client certificates against its truststore
- Only accepts connections from clients with valid certificates in the truststore

### MVC API (Client)

**Location**: `acme-api-mvc/src/main/resources/ssl/`

**Configuration** (`application.yml`):

```yaml
auth:
  service:
    base-url: https://localhost:8082
    ssl:
      enabled: true
      truststore:
        path: classpath:ssl/auth-service-truststore.jks
        password: changeit
      keystore:
        path: classpath:ssl/api-mvc-keystore.jks
        password: changeit
```

**Role**:

- Presents its client certificate to the auth service (from keystore)
- Verifies the auth service's server certificate against its truststore
- Only connects to servers with valid certificates in the truststore

### WebFlux API (Client)

**Location**: `acme-api-webflux/src/main/resources/ssl/`

**Configuration**: Same as MVC API, but uses `api-webflux-keystore.jks`

**Role**: Identical to MVC API, but for the reactive WebFlux stack

## SSL Configuration Implementation

The SSL configuration is centralized in the **security core module**:

**Location**: `acme-security/acme-security-core/src/main/java/org/acme/security/core/config/SslConfig.java`

This configuration class:

- Loads keystore and truststore from application properties
- Creates an `SSLContext` with both client and server certificate support
- Configures Apache HttpClient 5 for SSL/TLS communication
- Provides a `ClientHttpRequestFactory` bean used by `RestClient` in the auth client

The configuration is conditionally enabled via:

```yaml
auth.service.ssl.enabled: true
```

## Communication Flow

### 1. Client Initiates Connection

When the MVC or WebFlux API needs to authenticate a user:

```none
API → AuthServiceClient → RestClient → HTTPS Request
```

### 2. TLS Handshake

1. **Client Hello**: API sends its client certificate (from keystore)
2. **Server Hello**: Auth service sends its server certificate (from keystore)
3. **Certificate Verification**:
   - Client verifies server certificate against its truststore
   - Server verifies client certificate against its truststore
4. **Key Exchange**: Both parties establish encrypted session keys
5. **Encrypted Communication**: All subsequent data is encrypted

### 3. Request Processing

Once the mTLS handshake is complete:

- The auth service processes the request
- Returns user information over the encrypted connection
- The API uses the response to build the security context

## Certificate Generation

Certificates are generated using the script:

**Location**: `scripts/generate-ssl-certs.sh`

**What it does**:

1. Generates a keystore for the auth service (server)
2. Generates keystores for MVC and WebFlux APIs (clients)
3. Exports certificates from each keystore
4. Creates a shared truststore containing all certificates
5. Copies keystores and truststore to respective `src/main/resources/ssl/` directories

**To regenerate certificates**:

```bash
./scripts/generate-ssl-certs.sh
```

## Security Benefits

1. **Mutual Authentication**: Both client and server verify each other's identity
2. **Encryption**: All communication is encrypted in transit
3. **Certificate Pinning**: Only services with certificates in the truststore can communicate
4. **No Password-Based Auth**: Certificates replace username/password for service-to-service communication

## Troubleshooting

### Certificate Expiration

Certificates are valid for 365 days by default. To check expiration:

```bash
keytool -list -v -keystore <keystore>.jks -storepass changeit
```

### Connection Refused

If you see SSL handshake failures:

1. Verify certificates are in the correct locations
2. Check that `client-auth: need` is set on the server
3. Ensure the truststore contains all necessary certificates
4. Verify keystore passwords match configuration

### Certificate Mismatch

If the server rejects client certificates:

1. Verify the client's certificate is in the server's truststore
2. Check that certificate aliases match
3. Ensure certificates haven't expired

## References

- [Spring Boot SSL Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.webserver.configure-ssl)
- [Apache HttpClient 5 SSL](https://hc.apache.org/httpcomponents-client-5.1.x/current/httpclient5/apidocs/org/apache/hc/client5/http/ssl/package-summary.html)
- [Java Keytool Documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/keytool.html)
