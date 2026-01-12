<!-- omit in toc -->
# Kubernetes Setup for X.509 Client Certificate Authentication

This directory contains Kubernetes manifests for testing X.509 client certificate authentication with Minikube.

<!-- omit in toc -->
## Table of Contents

- [Directory Structure](#directory-structure)
- [Prerequisites](#prerequisites)
- [Quick Start (Automated)](#quick-start-automated)
- [Manual Setup](#manual-setup)
  - [1. Create Namespaces](#1-create-namespaces)
  - [2. Create CA Chain Secret](#2-create-ca-chain-secret)
  - [3. Create TLS Secret for Ingress](#3-create-tls-secret-for-ingress)
  - [4. Deploy Test Service](#4-deploy-test-service)
  - [5. Deploy Ingress](#5-deploy-ingress)
  - [6. Create TLS Secret (if not already created)](#6-create-tls-secret-if-not-already-created)
  - [7. Expose Ingress](#7-expose-ingress)
  - [8. Configure /etc/hosts](#8-configure-etchosts)
  - [9. Test](#9-test)
- [Deploying All Services](#deploying-all-services)
- [Component Details](#component-details)
  - [Infrastructure (`infrastructure/`)](#infrastructure-infrastructure)
  - [LDAP (`ldap/`)](#ldap-ldap)
  - [Auth Service (`auth/`)](#auth-service-auth)
  - [API (`api/`)](#api-api)
  - [Database (`database/`)](#database-database)
  - [Test (`test/`)](#test-test)
- [Troubleshooting](#troubleshooting)
- [Scripts](#scripts)
- [Verification](#verification)
- [Next Steps](#next-steps)

## Directory Structure

The Kubernetes manifests are organized by component:

```none
acme-infrastructure/
├── deployments/             # Application deployments
│   ├── ldap.yaml
│   ├── auth-service-ldap.yaml
│   ├── api-mvc.yaml
│   └── postgres-jpa.yaml
├── docker/                  # Custom Docker images
│   └── ldap/
│       └── Dockerfile      # Custom LDAP image with Kubernetes fixes
├── infrastructure/          # Shared infrastructure (Ingress, Namespaces, Secrets)
│   ├── namespace.yaml
│   ├── ingress.yaml
│   ├── ingress-secret.yaml
│   └── ingress-tls-secret.yaml
├── test/                    # Test services and alternative configurations
│   ├── test-service.yaml
│   ├── ingress-api-test.yaml
│   └── ingress-no-snippets.yaml
├── scripts/                 # Deployment and setup scripts
│   ├── deploy.sh
│   ├── setup-minikube.sh
│   ├── enable-snippets.sh
│   ├── port-forward.sh
│   └── certs/              # Certificate generation scripts
│       ├── generate-ca.sh
│       ├── generate-user-cert.sh
│       └── setup-all-certs.sh
├── certs/                   # Certificate files (gitignored)
│   ├── ca/                  # CA certificates
│   └── users/               # User certificates
└── README.md               # This file
```

## Prerequisites

1. **Minikube installed and running**

   ```bash
   minikube start
   ```

2. **NGINX Ingress Controller installed**

   ```bash
   minikube addons enable ingress
   ```

3. **Certificates generated**

   ```bash
   ./acme-infrastructure/scripts/certs/setup-all-certs.sh
   ```

4. **Root CA and user certificates imported into browser** (see `docs/X509.md`)

## Quick Start (Automated)

Run the setup script:

```bash
./acme-infrastructure/scripts/setup-minikube.sh
```

This will:

- Start Minikube (if not running)
- Enable NGINX Ingress
- Create namespaces
- Create secrets
- Deploy test service and Ingress
- Display next steps

## Manual Setup

### 1. Create Namespaces

```bash
kubectl apply -f acme-infrastructure/infrastructure/namespace.yaml
```

### 2. Create CA Chain Secret

The Ingress needs the CA chain to validate client certificates:

```bash
kubectl create secret generic ca-chain-secret \
  --from-file=ca-chain.crt=acme-infrastructure/certs/ca/ca-chain.crt \
  -n acme-ingress
```

### 3. Create TLS Secret for Ingress

The Ingress needs a server certificate for HTTPS. For testing, we can use the auth-service certificate:

```bash
kubectl create secret tls ingress-tls-secret \
  --cert=monitoring/certs/auth-service.crt \
  --key=monitoring/certs/auth-service.key \
  -n acme-ingress
```

### 4. Deploy Test Service

```bash
kubectl apply -f acme-infrastructure/test/test-service.yaml
```

### 5. Deploy Ingress

```bash
kubectl apply -f acme-infrastructure/infrastructure/ingress.yaml
```

### 6. Create TLS Secret (if not already created)

The Ingress needs a TLS secret for HTTPS. If the setup script didn't create it:

```bash
kubectl create secret tls ingress-tls-secret \
  --cert=monitoring/certs/auth-service.crt \
  --key=monitoring/certs/auth-service.key \
  -n acme-ingress
```

### 7. Expose Ingress

**Option A: Port Forward (Recommended - No Sudo Required)**

This is the simplest method and works immediately:

```bash
./acme-infrastructure/scripts/port-forward.sh
```

This will forward `localhost:8443` to the Ingress controller's HTTPS port (443).

**Option B: Minikube Tunnel (Requires Sudo)**

```bash
sudo minikube tunnel
```

This will assign an external IP to the Ingress LoadBalancer service.

### 8. Configure /etc/hosts

Add the following line to `/etc/hosts`:

```bash
127.0.0.1 acme.local
```

Or if using `minikube tunnel`:

```bash
$(minikube ip) acme.local
```

### 9. Test

Open your browser and navigate to:

```none
https://acme.local:8443/test
```

(Use port 8443 if using port-forward, or port 443 if using minikube tunnel)

You should be prompted to select a client certificate. After selecting it, you should see JSON with the certificate DNs.

## Deploying All Services

To deploy all services (LDAP, Auth Service, API, Database):

```bash
./acme-infrastructure/scripts/deploy.sh
```

This will:

1. Build Docker images using Minikube's Docker daemon
2. Deploy all services to Kubernetes
3. Wait for deployments to be ready
4. Show pod status

## Component Details

### Infrastructure (`infrastructure/`)

- **namespace.yaml**: Defines `acme-ingress` and `acme-apps` namespaces
- **ingress.yaml**: Main Ingress configuration with mTLS and header forwarding
- **ingress-secret.yaml**: CA chain secret for client certificate validation
- **ingress-tls-secret.yaml**: TLS secret for Ingress HTTPS

### LDAP (`ldap/`)

- **ldap.yaml**: OpenLDAP StatefulSet and Service
- **Dockerfile.ldap-fix**: Custom LDAP image with Kubernetes compatibility fixes

### Auth Service (`auth/`)

- **auth-service-ldap.yaml**: Authentication service deployment and service

### API (`api/`)

- **api-mvc.yaml**: MVC API deployment and service

### Database (`database/`)

- **postgres-jpa.yaml**: PostgreSQL deployment and service for JPA

### Test (`test/`)

- **test-service.yaml**: Simple NGINX service for testing header extraction
- **ingress-api-test.yaml**: Alternative Ingress for testing API routing
- **ingress-no-snippets.yaml**: Ingress without snippet annotations (fallback)

## Troubleshooting

See [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) for common issues and solutions.

## Scripts

- **setup-minikube.sh**: Automated setup of Minikube, Ingress, and test service
- **deploy.sh**: Build and deploy all application services
- **enable-snippets.sh**: Enable snippet annotations in NGINX Ingress Controller
- **port-forward.sh**: Port-forward Ingress controller for local access

## Verification

Check that everything is running:

```bash
# Check pods
kubectl get pods -n acme-apps

# Check services
kubectl get svc -n acme-apps

# Check ingress
kubectl get ingress -n acme-apps

# View logs
kubectl logs -n acme-apps <pod-name>
```

## Next Steps

1. Import user certificates into your browser (see `docs/X509.md`)
2. Test the endpoints:
   - `https://acme.local:8443/test` - Test header extraction
   - `https://acme.local:8443/api/v1/books` - API endpoint
3. Check logs if issues occur:

   ```bash
   kubectl logs -n ingress-nginx -l app.kubernetes.io/component=controller
   ```
