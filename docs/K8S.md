<!-- omit in toc -->
# Kubernetes Migration Plan

This document outlines the plan for migrating the Acme application from Docker Compose to Kubernetes, including X509 certificate-based authentication via NGINX Ingress.

<!-- omit in toc -->
## Table of Contents

- [Overview](#overview)
- [Goals](#goals)
- [Architecture Overview](#architecture-overview)
- [Phase 1: X509 Certificate Generation](#phase-1-x509-certificate-generation)
  - [Certificate Authority (CA) Setup](#certificate-authority-ca-setup)
  - [User Certificate Generation](#user-certificate-generation)
  - [Certificate Distribution](#certificate-distribution)
  - [Browser Configuration](#browser-configuration)
- [Phase 2: Kubernetes Cluster Setup](#phase-2-kubernetes-cluster-setup)
  - [Cluster Requirements](#cluster-requirements)
  - [Infrastructure Components](#infrastructure-components)
  - [Namespace Strategy](#namespace-strategy)
  - [Network Policies](#network-policies)
- [Phase 3: NGINX Ingress Configuration](#phase-3-nginx-ingress-configuration)
  - [mTLS Configuration](#mtls-configuration)
  - [DN Extraction from Certificate](#dn-extraction-from-certificate)
  - [Header Forwarding](#header-forwarding)
  - [Ingress Rules](#ingress-rules)
- [Phase 4: Application Deployment](#phase-4-application-deployment)
  - [Database Services](#database-services)
  - [LDAP Service](#ldap-service)
  - [Auth Services](#auth-services)
  - [API Services](#api-services)
  - [UI Service](#ui-service)
  - [Monitoring Services](#monitoring-services)
- [Phase 5: Configuration Management](#phase-5-configuration-management)
  - [ConfigMaps](#configmaps)
  - [Secrets](#secrets)
  - [Environment Variables](#environment-variables)
- [Phase 6: Storage and Persistence](#phase-6-storage-and-persistence)
  - [Database Volumes](#database-volumes)
  - [LDAP Data](#ldap-data)
  - [Monitoring Data](#monitoring-data)
- [Phase 7: Service Discovery and Networking](#phase-7-service-discovery-and-networking)
  - [Service Definitions](#service-definitions)
  - [Internal Communication](#internal-communication)
  - [External Access](#external-access)
- [Phase 8: Health Checks and Readiness](#phase-8-health-checks-and-readiness)
  - [Liveness Probes](#liveness-probes)
  - [Readiness Probes](#readiness-probes)
  - [Startup Probes](#startup-probes)
- [Phase 9: Resource Management](#phase-9-resource-management)
  - [Resource Requests and Limits](#resource-requests-and-limits)
  - [Horizontal Pod Autoscaling](#horizontal-pod-autoscaling)
  - [Vertical Pod Autoscaling](#vertical-pod-autoscaling)
- [Phase 10: Monitoring and Logging](#phase-10-monitoring-and-logging)
  - [Prometheus Integration](#prometheus-integration)
  - [Grafana Dashboards](#grafana-dashboards)
  - [Log Aggregation](#log-aggregation)
- [Migration Strategy](#migration-strategy)
  - [Phase-by-Phase Approach](#phase-by-phase-approach)
  - [Parallel Running](#parallel-running)
  - [Data Migration](#data-migration)
- [Testing Plan](#testing-plan)
  - [Unit Tests](#unit-tests)
  - [Integration Tests](#integration-tests)
  - [End-to-End Tests](#end-to-end-tests)
  - [Load Tests](#load-tests)
- [Rollback Plan](#rollback-plan)
  - [Immediate Rollback](#immediate-rollback)
  - [Data Rollback](#data-rollback)
  - [Certificate Rollback](#certificate-rollback)
- [Timeline](#timeline)
- [Dependencies and Prerequisites](#dependencies-and-prerequisites)
  - [Required Tools](#required-tools)
  - [Required Knowledge](#required-knowledge)
  - [External Dependencies](#external-dependencies)
- [Risks and Mitigation](#risks-and-mitigation)
  - [Risk: Certificate Management Complexity](#risk-certificate-management-complexity)
  - [Risk: mTLS Configuration Issues](#risk-mtls-configuration-issues)
  - [Risk: Service Discovery Problems](#risk-service-discovery-problems)
  - [Risk: Data Loss During Migration](#risk-data-loss-during-migration)
  - [Risk: Performance Degradation](#risk-performance-degradation)
  - [Risk: Network Policy Misconfiguration](#risk-network-policy-misconfiguration)
- [Future Enhancements](#future-enhancements)
  - [Short-term](#short-term)
  - [Long-term](#long-term)

## Overview

This migration will move the Acme application from a Docker Compose-based deployment to a Kubernetes cluster. The key enhancement is implementing X509 certificate-based authentication where:

1. Each LDAP user receives an X509 client certificate
2. Users import certificates into their browsers
3. NGINX Ingress requires client certificates (mTLS)
4. NGINX extracts the user's DN from the certificate
5. NGINX forwards the DN as an `x-dn` header to backend services
6. Backend services use the `x-dn` header for authentication (existing behavior)

## Goals

- **Security**: Replace header-based authentication with X509 mTLS at the ingress
- **Scalability**: Enable horizontal scaling of services
- **Reliability**: Improve availability with health checks and auto-recovery
- **Observability**: Enhanced monitoring and logging in K8s ecosystem
- **Maintainability**: Infrastructure as code with K8s manifests
- **Zero Downtime**: Gradual migration with rollback capability

## Architecture Overview

```none
┌─────────────────────────────────────────────────────────────┐
│                    User Browser                             │
│  (X509 Client Certificate installed)                        │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTPS + mTLS
                       │ (Client cert required)
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              NGINX Ingress Controller                       │
│  - Validates client certificate                             │
│  - Extracts DN from certificate Subject/SAN                 │
│  - Adds x-dn header: cn=john doe,ou=engineering,...         │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│   UI Pod    │ │  API-MVC    │ │ API-WebFlux │
│  (Next.js)  │ │    Pod      │ │    Pod      │
└──────┬──────┘ └──────┬──────┘ └──────┬──────┘
       │               │                │
       │               └────────┬───────┘
       │                        │
       │                        ▼
       │              ┌──────────────────┐
       │              │  Auth Service    │
       │              │  (LDAP or DB)    │
       │              └────────┬─────────┘
       │                       │
       └───────────────────────┼───────────┐
                               │           │
                    ┌──────────┴──┐  ┌─────┴──────┐
                    │   LDAP Pod  │  │ PostgreSQL │
                    │  (OpenLDAP) │  │    Pods    │
                    └─────────────┘  └────────────┘
```

## Phase 1: X509 Certificate Generation

### Certificate Authority (CA) Setup

**Objective**: Create a root CA and intermediate CA for signing user certificates.

**Steps**:

1. **Generate Root CA**

   ```bash
   # Create CA directory structure
   mkdir -p k8s/certs/ca/{root,intermediate}
   
   # Generate root CA private key
   openssl genrsa -out k8s/certs/ca/root/ca-root.key 4096
   
   # Generate root CA certificate (valid for 10 years)
   openssl req -new -x509 -days 3650 -key k8s/certs/ca/root/ca-root.key \
     -out k8s/certs/ca/root/ca-root.crt \
     -subj "/CN=Acme Root CA/O=Acme Corp/C=US"
   ```

2. **Generate Intermediate CA**

   ```bash
   # Generate intermediate CA private key
   openssl genrsa -out k8s/certs/ca/intermediate/ca-intermediate.key 4096
   
   # Generate intermediate CA CSR
   openssl req -new -key k8s/certs/ca/intermediate/ca-intermediate.key \
     -out k8s/certs/ca/intermediate/ca-intermediate.csr \
     -subj "/CN=Acme Intermediate CA/O=Acme Corp/C=US"
   
   # Sign intermediate CA with root CA (valid for 5 years)
   openssl x509 -req -days 1825 \
     -in k8s/certs/ca/intermediate/ca-intermediate.csr \
     -CA k8s/certs/ca/root/ca-root.crt \
     -CAkey k8s/certs/ca/root/ca-root.key \
     -CAcreateserial \
     -out k8s/certs/ca/intermediate/ca-intermediate.crt \
     -extensions v3_intermediate_ca \
     -extfile <(cat <<EOF
     [v3_intermediate_ca]
     basicConstraints = critical,CA:true
     keyUsage = critical,keyCertSign,cRLSign
     EOF
     )
   ```

3. **Create Certificate Chain**

   ```bash
   # Combine root and intermediate for full chain
   cat k8s/certs/ca/intermediate/ca-intermediate.crt \
       k8s/certs/ca/root/ca-root.crt > k8s/certs/ca/ca-chain.crt
   ```

### User Certificate Generation

**Objective**: Generate client certificates for each LDAP user with DN embedded.

**Certificate Requirements**:

- **Subject**: Include user's LDAP DN (e.g., `CN=John Doe,OU=Engineering,OU=Users,DC=corp,DC=acme,DC=org`)
- **Subject Alternative Name (SAN)**: Alternative DN formats for flexibility
- **Key Usage**: `digitalSignature`, `keyEncipherment`
- **Extended Key Usage**: `clientAuth`
- **Validity**: 1 year (renewable)

**Script**: `scripts/generate-user-cert.sh`

```bash
#!/bin/bash
# Generate X509 certificate for LDAP user
# Usage: ./generate-user-cert.sh "cn=john doe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org" jdoe

USER_DN="$1"
USER_ID="$2"  # e.g., jdoe

if [ -z "$USER_DN" ] || [ -z "$USER_ID" ]; then
  echo "Usage: $0 <user_dn> <user_id>"
  exit 1
fi

CERT_DIR="k8s/certs/users"
mkdir -p "$CERT_DIR"

# Generate user private key
openssl genrsa -out "$CERT_DIR/${USER_ID}.key" 2048

# Create certificate config with DN in Subject
cat > "$CERT_DIR/${USER_ID}.conf" <<EOF
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req
prompt = no

[req_distinguished_name]
$(echo "$USER_DN" | sed 's/,/\n/g' | sed 's/=/ = /')

[v3_req]
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = clientAuth
subjectAltName = @alt_names

[alt_names]
# Alternative DN formats
DN.1 = $USER_DN
EOF

# Generate certificate signing request
openssl req -new -key "$CERT_DIR/${USER_ID}.key" \
  -out "$CERT_DIR/${USER_ID}.csr" \
  -config "$CERT_DIR/${USER_ID}.conf"

# Sign certificate with intermediate CA (valid for 1 year)
openssl x509 -req -days 365 \
  -in "$CERT_DIR/${USER_ID}.csr" \
  -CA k8s/certs/ca/intermediate/ca-intermediate.crt \
  -CAkey k8s/certs/ca/intermediate/ca-intermediate.key \
  -CAcreateserial \
  -out "$CERT_DIR/${USER_ID}.crt" \
  -extensions v3_req \
  -extfile "$CERT_DIR/${USER_ID}.conf"

# Create PKCS#12 bundle for browser import
openssl pkcs12 -export \
  -out "$CERT_DIR/${USER_ID}.p12" \
  -inkey "$CERT_DIR/${USER_ID}.key" \
  -in "$CERT_DIR/${USER_ID}.crt" \
  -certfile k8s/certs/ca/ca-chain.crt \
  -passout pass:  # No password for easier import

echo "Certificate generated: $CERT_DIR/${USER_ID}.p12"
echo "Import this file into your browser's certificate store"
```

**Batch Generation Script**: `scripts/generate-all-user-certs.sh`

```bash
#!/bin/bash
# Generate certificates for all LDAP users
# Reads from LDAP and generates certificates

# Query LDAP for all users
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=corp,dc=acme,dc=org" \
  -w admin \
  -b "ou=users,dc=corp,dc=acme,dc=org" \
  "(objectClass=inetOrgPerson)" \
  dn uid | \
  grep -E "^dn:|^uid:" | \
  while read line; do
    if [[ $line == dn:* ]]; then
      USER_DN="${line#dn: }"
    elif [[ $line == uid:* ]]; then
      USER_ID="${line#uid: }"
      echo "Generating certificate for $USER_ID..."
      ./scripts/generate-user-cert.sh "$USER_DN" "$USER_ID"
    fi
  done
```

### Certificate Distribution

**Options**:

1. **Manual Distribution**
   - Provide `.p12` files to users via secure channel
   - Users import into browser certificate store

2. **Automated Distribution** (Future)
   - Certificate management service
   - Self-service portal for certificate requests
   - Integration with LDAP for automatic provisioning

**Browser Import Instructions**:

- **Chrome/Edge**: Settings → Privacy and Security → Security → Manage certificates → Import
- **Firefox**: Settings → Privacy & Security → Certificates → View Certificates → Import
- **Safari**: Keychain Access → Import Items

### Browser Configuration

**CA Trust**:

- Users must install the root CA certificate in their browser's trusted root store
- This allows browsers to trust certificates signed by the CA

**Certificate Selection**:

- Browsers will automatically present client certificates when requested by the server
- Users may need to select the correct certificate if multiple are available

## Phase 2: Kubernetes Cluster Setup

### Cluster Requirements

**Minimum Requirements**:

- **Nodes**: 3 (1 control plane, 2 workers minimum for HA)
- **CPU**: 4 cores per node
- **Memory**: 8GB per node
- **Storage**: 100GB per node
- **Network**: CNI plugin (Calico, Flannel, or Cilium)

**Recommended for Production**:

- **Nodes**: 5+ (3 control plane, 3+ workers)
- **CPU**: 8+ cores per node
- **Memory**: 16GB+ per node
- **Storage**: 200GB+ per node with SSD

**Options**:

- **Local Development**: minikube, kind, k3d
- **Cloud**: EKS (AWS), GKE (GCP), AKS (Azure)
- **On-Premise**: kubeadm, Rancher, OpenShift

### Infrastructure Components

**Required Components**:

1. **NGINX Ingress Controller**

   ```bash
   kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
   ```

2. **Cert-Manager** (for TLS certificate management)

   ```bash
   kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml
   ```

3. **Metrics Server** (for HPA)

   ```bash
   kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
   ```

4. **Storage Class** (for persistent volumes)
   - Depends on cloud provider or local storage solution

### Namespace Strategy

**Namespaces**:

- `acme-infra`: Databases, LDAP, monitoring
- `acme-apps`: Application services (auth, APIs, UI)
- `acme-ingress`: NGINX Ingress Controller
- `acme-monitoring`: Prometheus, Grafana

**Benefits**:

- Resource isolation
- Network policy enforcement
- RBAC separation
- Easier management

### Network Policies

**Policy Strategy**:

- Deny all by default
- Allow ingress traffic only from NGINX
- Allow egress to databases/LDAP only from authorized pods
- Allow monitoring scraping from Prometheus

**Example Policy**:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: api-mvc-policy
  namespace: acme-apps
spec:
  podSelector:
    matchLabels:
      app: api-mvc
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: acme-ingress
    - podSelector:
        matchLabels:
          app: nginx-ingress
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: acme-infra
    - podSelector:
        matchLabels:
          app: postgres-jpa
  - to:
    - namespaceSelector:
        matchLabels:
          name: acme-infra
    - podSelector:
        matchLabels:
          app: auth-service
```

## Phase 3: NGINX Ingress Configuration

### mTLS Configuration

**NGINX Configuration**:

- Require client certificate authentication
- Validate certificate chain against CA
- Extract DN from certificate Subject

**ConfigMap for NGINX**:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: nginx-mtls-config
  namespace: acme-ingress
data:
  nginx.conf: |
    # Enable SSL and client certificate verification
    ssl_verify_client on;
    ssl_verify_depth 2;
    ssl_client_certificate /etc/nginx/certs/ca-chain.crt;
    
    # Extract DN from certificate
    map $ssl_client_s_dn $client_dn {
      default $ssl_client_s_dn;
    }
    
    # Normalize DN format (remove spaces, handle case)
    map $client_dn $normalized_dn {
      ~^(.+)$ $1;
    }
```

**Secret for CA Chain**:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: ca-chain-secret
  namespace: acme-ingress
type: Opaque
data:
  ca-chain.crt: <base64-encoded-ca-chain.crt>
```

### DN Extraction from Certificate

**NGINX Variables**:

- `$ssl_client_s_dn`: Full Subject DN from certificate
- Format: `CN=John Doe,OU=Engineering,OU=Users,DC=corp,DC=acme,DC=org`

**Normalization**:

- May need to handle case differences
- Remove extra spaces
- Ensure consistent format

**Lua Script** (if needed for complex extraction):

```lua
-- Extract and normalize DN
local dn = ngx.var.ssl_client_s_dn
if dn then
  -- Convert to lowercase for consistency
  dn = string.lower(dn)
  -- Remove extra spaces
  dn = string.gsub(dn, "%s+", " ")
  ngx.var.normalized_dn = dn
end
```

### Header Forwarding

**Ingress Annotation**:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: acme-ingress
  namespace: acme-apps
  annotations:
    nginx.ingress.kubernetes.io/auth-tls-verify-client: "on"
    nginx.ingress.kubernetes.io/auth-tls-secret: "acme-ingress/ca-chain-secret"
    nginx.ingress.kubernetes.io/configuration-snippet: |
      more_set_headers "x-dn: $ssl_client_s_dn";
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - acme.example.com
    secretName: acme-tls-secret
  rules:
  - host: acme.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: ui-service
            port:
              number: 3001
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: api-mvc-service
            port:
              number: 8080
```

### Ingress Rules

**Routing Strategy**:

- `/` → UI service (Next.js)
- `/api/v1/*` → API services (MVC or WebFlux)
- `/api/v1/users/*` → Auth service (if direct access needed)

**Path-based Routing**:

```yaml
spec:
  rules:
  - host: acme.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: ui-service
            port:
              number: 3001
      - path: /api/v1/books
        pathType: Prefix
        backend:
          service:
            name: api-mvc-service
            port:
              number: 8080
      - path: /api/v1/users
        pathType: Prefix
        backend:
          service:
            name: auth-service
            port:
              number: 8082
```

## Phase 4: Application Deployment

### Database Services

**PostgreSQL Deployments**:

- `postgres-jpa`: For MVC API
- `postgres-r2dbc`: For WebFlux API
- `postgres-auth`: For DB-based auth service

**StatefulSets** (for persistent storage):

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres-jpa
  namespace: acme-infra
spec:
  serviceName: postgres-jpa
  replicas: 1
  selector:
    matchLabels:
      app: postgres-jpa
  template:
    metadata:
      labels:
        app: postgres-jpa
    spec:
      containers:
      - name: postgres
        image: postgres:17-alpine
        env:
        - name: POSTGRES_DB
          value: acme_jpa
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: postgres-secrets
              key: username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secrets
              key: password
        volumeMounts:
        - name: postgres-data
          mountPath: /var/lib/postgresql/data
  volumeClaimTemplates:
  - metadata:
      name: postgres-data
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 10Gi
```

### LDAP Service

**StatefulSet for OpenLDAP**:

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: ldap
  namespace: acme-infra
spec:
  serviceName: ldap
  replicas: 1
  selector:
    matchLabels:
      app: ldap
  template:
    metadata:
      labels:
        app: ldap
    spec:
      containers:
      - name: ldap
        image: osixia/openldap:latest
        env:
        - name: LDAP_BASE_DN
          value: "dc=corp,dc=acme,dc=org"
        - name: LDAP_ADMIN_PASSWORD
          valueFrom:
            secretKeyRef:
              name: ldap-secrets
              key: admin-password
        volumeMounts:
        - name: ldap-data
          mountPath: /var/lib/ldap
        - name: ldap-config
          mountPath: /etc/ldap/slapd.d
        - name: ldif-import
          mountPath: /ldif-import
          readOnly: true
      volumes:
      - name: ldif-import
        configMap:
          name: ldap-ldif
  volumeClaimTemplates:
  - metadata:
      name: ldap-data
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 5Gi
```

### Auth Services

**Deployment for Auth-LDAP**:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-ldap
  namespace: acme-apps
spec:
  replicas: 2
  selector:
    matchLabels:
      app: auth-ldap
  template:
    metadata:
      labels:
        app: auth-ldap
    spec:
      containers:
      - name: auth-ldap
        image: acme/auth-service-ldap:latest
        env:
        - name: SPRING_LDAP_URLS
          value: "ldap://ldap.acme-infra.svc.cluster.local:389"
        - name: SPRING_LDAP_BASE
          value: "dc=corp,dc=acme,dc=org"
        - name: SPRING_LDAP_USERNAME
          valueFrom:
            secretKeyRef:
              name: ldap-secrets
              key: admin-dn
        - name: SPRING_LDAP_PASSWORD
          valueFrom:
            secretKeyRef:
              name: ldap-secrets
              key: admin-password
        ports:
        - containerPort: 8082
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8082
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8082
          initialDelaySeconds: 30
          periodSeconds: 5
```

### API Services

**Deployment for API-MVC**:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-mvc
  namespace: acme-apps
spec:
  replicas: 3
  selector:
    matchLabels:
      app: api-mvc
  template:
    metadata:
      labels:
        app: api-mvc
    spec:
      containers:
      - name: api-mvc
        image: acme/api-mvc:latest
        env:
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres-jpa.acme-infra.svc.cluster.local:5432/acme_jpa"
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: postgres-secrets
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secrets
              key: password
        - name: AUTH_SERVICE_URL
          value: "http://auth-ldap.acme-apps.svc.cluster.local:8082"
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
```

### UI Service

**Deployment for Next.js UI**:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ui
  namespace: acme-apps
spec:
  replicas: 2
  selector:
    matchLabels:
      app: ui
  template:
    metadata:
      labels:
        app: ui
    spec:
      containers:
      - name: ui
        image: acme/ui:latest
        env:
        - name: NEXT_PUBLIC_API_URL
          value: "https://acme.example.com/api"
        ports:
        - containerPort: 3001
        resources:
          requests:
            memory: "256Mi"
            cpu: "100m"
          limits:
            memory: "512Mi"
            cpu: "500m"
```

### Monitoring Services

**Prometheus Deployment**:

- StatefulSet for persistent metrics storage
- ConfigMap for scrape configuration
- ServiceMonitor CRDs for service discovery

**Grafana Deployment**:

- Deployment with persistent volume for dashboards
- ConfigMap for datasource provisioning
- ConfigMap for dashboard provisioning

## Phase 5: Configuration Management

### ConfigMaps

**Application ConfigMaps**:

- Database connection strings
- LDAP configuration
- Feature flags
- Non-sensitive application settings

**Example**:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: api-mvc-config
  namespace: acme-apps
data:
  application.yml: |
    spring:
      datasource:
        url: jdbc:postgresql://postgres-jpa.acme-infra.svc.cluster.local:5432/acme_jpa
    management:
      endpoints:
        web:
          exposure:
            include: health,metrics,prometheus
```

### Secrets

**Secret Management**:

- Database passwords
- LDAP credentials
- TLS certificates
- API keys

**Example**:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: postgres-secrets
  namespace: acme-infra
type: Opaque
stringData:
  username: acme_user
  password: acme_password
```

**Best Practices**:

- Use `stringData` for plain text (auto-encoded)
- Use external secret management (Sealed Secrets, External Secrets Operator, Vault)
- Rotate secrets regularly
- Never commit secrets to git

### Environment Variables

**Strategy**:

- Sensitive: Secrets
- Non-sensitive: ConfigMaps
- Service-specific: Deployment env vars
- Shared: ConfigMap references

## Phase 6: Storage and Persistence

### Database Volumes

**Storage Classes**:

- `fast-ssd`: For database workloads
- `standard`: For general purpose

**Volume Claims**:

- Persistent volumes for each database
- Backup strategy (Velero, database-specific tools)

### LDAP Data

**LDAP Storage**:

- Persistent volume for `/var/lib/ldap`
- Persistent volume for `/etc/ldap/slapd.d`
- Regular backups of LDAP data

### Monitoring Data

**Prometheus Storage**:

- Large persistent volume (100GB+)
- Retention policy configuration
- Long-term storage (Thanos, Cortex) for production

## Phase 7: Service Discovery and Networking

### Service Definitions

**ClusterIP Services** (internal):

```yaml
apiVersion: v1
kind: Service
metadata:
  name: api-mvc-service
  namespace: acme-apps
spec:
  selector:
    app: api-mvc
  ports:
  - port: 8080
    targetPort: 8080
  type: ClusterIP
```

**Service Naming**:

- Format: `<service-name>.<namespace>.svc.cluster.local`
- Example: `api-mvc.acme-apps.svc.cluster.local`

### Internal Communication

**Service-to-Service**:

- Use service DNS names
- No need for external IPs
- Network policies control access

**Example**:

```yaml
env:
- name: AUTH_SERVICE_URL
  value: "http://auth-ldap.acme-apps.svc.cluster.local:8082"
```

### External Access

**Ingress Only**:

- All external traffic through NGINX Ingress
- No NodePort or LoadBalancer services for apps
- Ingress handles TLS termination and mTLS

## Phase 8: Health Checks and Readiness

### Liveness Probes

**Purpose**: Restart unhealthy containers

**Configuration**:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

### Readiness Probes

**Purpose**: Remove pod from service endpoints when not ready

**Configuration**:

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

### Startup Probes

**Purpose**: Give slow-starting containers time to initialize

**Configuration**:

```yaml
startupProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 30  # 5 minutes total
```

## Phase 9: Resource Management

### Resource Requests and Limits

**Requests**: Guaranteed resources
**Limits**: Maximum resources

**Example**:

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

### Horizontal Pod Autoscaling

**HPA Configuration**:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-mvc-hpa
  namespace: acme-apps
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-mvc
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### Vertical Pod Autoscaling

**VPA Configuration** (optional):

- Automatically adjusts resource requests/limits
- Requires VPA operator installation

## Phase 10: Monitoring and Logging

### Prometheus Integration

**ServiceMonitor CRDs**:

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: api-mvc-metrics
  namespace: acme-apps
spec:
  selector:
    matchLabels:
      app: api-mvc
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

### Grafana Dashboards

**Dashboard Provisioning**:

- ConfigMap for dashboard definitions
- Auto-provisioned on Grafana startup
- Same dashboards as Docker Compose setup

### Log Aggregation

**Options**:

- **Loki**: Lightweight, Prometheus-like for logs
- **ELK Stack**: Elasticsearch, Logstash, Kibana
- **Fluentd/Fluent Bit**: Log forwarders
- **Cloud-native**: CloudWatch, Stackdriver, Azure Monitor

**Strategy**:

- Sidecar containers for log collection
- Centralized log storage
- Log retention policies

## Migration Strategy

### Phase-by-Phase Approach

1. **Phase 1**: Set up K8s cluster (local or cloud)
2. **Phase 2**: Deploy infrastructure (databases, LDAP)
3. **Phase 3**: Generate and distribute X509 certificates
4. **Phase 4**: Configure NGINX Ingress with mTLS
5. **Phase 5**: Deploy auth services
6. **Phase 6**: Deploy API services
7. **Phase 7**: Deploy UI service
8. **Phase 8**: Migrate monitoring
9. **Phase 9**: Test end-to-end
10. **Phase 10**: Cutover and decommission Docker Compose

### Parallel Running

- Run Docker Compose and K8s in parallel during migration
- Use different ports/domains
- Gradually migrate traffic
- Validate functionality before full cutover

### Data Migration

**Databases**:

- Export data from Docker Compose
- Import into K8s databases
- Verify data integrity

**LDAP**:

- Export LDIF from Docker Compose
- Import into K8s LDAP
- Verify user and group data

## Testing Plan

### Unit Tests

- Certificate generation scripts
- DN extraction logic
- Configuration validation

### Integration Tests

- mTLS handshake
- DN header forwarding
- Service-to-service communication
- Database connectivity

### End-to-End Tests

- User certificate import
- Browser authentication flow
- API calls with x-dn header
- UI functionality

### Load Tests

- Horizontal scaling
- Resource limits
- Network policies
- Ingress performance

## Rollback Plan

### Immediate Rollback

- Keep Docker Compose running during migration
- Switch DNS/ingress back to Docker Compose
- Preserve K8s state for investigation

### Data Rollback

- Database backups before migration
- LDAP backup before migration
- Ability to restore from backups

### Certificate Rollback

- Keep old certificates valid
- Support both authentication methods temporarily

## Timeline

**Estimated Timeline**: 4-6 weeks

- **Week 1**: Cluster setup, certificate generation, NGINX Ingress configuration
- **Week 2**: Infrastructure deployment (databases, LDAP)
- **Week 3**: Application deployment (auth, APIs)
- **Week 4**: UI deployment, monitoring migration
- **Week 5**: Testing, optimization, documentation
- **Week 6**: Production cutover, monitoring, cleanup

## Dependencies and Prerequisites

### Required Tools

- `kubectl`: Kubernetes CLI
- `helm`: Package manager (optional)
- `kustomize`: Configuration management (optional)
- `openssl`: Certificate generation
- `ldapsearch`: LDAP querying

### Required Knowledge

- Kubernetes concepts (Pods, Services, Deployments, Ingress)
- NGINX configuration
- X509 certificate management
- mTLS/TLS concepts
- Network policies

### External Dependencies

- Kubernetes cluster (local or cloud)
- Domain name for ingress
- TLS certificate for ingress (Let's Encrypt or self-signed)
- Storage provisioner

## Risks and Mitigation

### Risk: Certificate Management Complexity

- **Mitigation**: Automated scripts, clear documentation, certificate rotation procedures

### Risk: mTLS Configuration Issues

- **Mitigation**: Thorough testing, fallback authentication method, clear error messages

### Risk: Service Discovery Problems

- **Mitigation**: Use DNS names, test connectivity, network policy validation

### Risk: Data Loss During Migration

- **Mitigation**: Comprehensive backups, parallel running, verification steps

### Risk: Performance Degradation

- **Mitigation**: Resource monitoring, load testing, performance baselines

### Risk: Network Policy Misconfiguration

- **Mitigation**: Start permissive, tighten gradually, test connectivity

## Future Enhancements

### Short-term

- Certificate auto-renewal
- Self-service certificate portal
- Enhanced monitoring dashboards
- Automated backup/restore

### Long-term

- Multi-cluster deployment
- Service mesh (Istio, Linkerd)
- GitOps (ArgoCD, Flux)
- Advanced autoscaling
- Disaster recovery procedures
- Certificate revocation list (CRL) support
- OCSP (Online Certificate Status Protocol) integration
