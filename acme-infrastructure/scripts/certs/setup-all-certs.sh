#!/bin/bash
# Unified certificate setup script
# Generates all certificates needed for:
# 1. Auth service HTTPS server
# 2. User certificates for browser authentication
# 3. API client certificates for mTLS
# 4. Prometheus client certificates
# All certificates share the same CA trust chain
#
# This script delegates to:
#   - generate-ca.sh (for CA generation)
#   - generate-user-cert.sh (for each user certificate)
# And replaces:
#   - generate-mtls-certs.sh (now uses same CA)
#   - copy-mtls-certs.sh (copying is done here)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CERT_DIR="$PROJECT_ROOT/acme-infrastructure/certs"
ROOT_CA_DIR="$CERT_DIR/ca/root"
INTERMEDIATE_CA_DIR="$CERT_DIR/ca/intermediate"
USER_CERT_DIR="$CERT_DIR/users"
SERVER_CERT_DIR="$PROJECT_ROOT/monitoring/certs"
KEYSTORE_PASSWORD="changeit"
TRUSTSTORE_PASSWORD="changeit"
VALIDITY_DAYS=365

echo "=========================================="
echo "Unified Certificate Setup"
echo "=========================================="
echo ""
echo "This script will generate:"
echo "  1. Root CA and Intermediate CA"
echo "  2. User certificates (for browser)"
echo "  3. Server certificates (for auth service HTTPS)"
echo "  4. API client certificates (for mTLS)"
echo "  5. Prometheus client certificates"
echo "  6. Truststore with CA chain"
echo ""
echo "All certificates will share the same CA trust chain."
echo ""

# Create directory structure
mkdir -p "$ROOT_CA_DIR" "$INTERMEDIATE_CA_DIR" "$USER_CERT_DIR" "$SERVER_CERT_DIR"

# ==========================================
# Step 1: Generate CA Chain (delegate to generate-ca.sh)
# ==========================================
echo "Step 1: Generating Certificate Authority (CA) chain..."
echo "---------------------------------------------------"

if [ -f "$ROOT_CA_DIR/ca-root.crt" ] || [ -f "$INTERMEDIATE_CA_DIR/ca-intermediate.crt" ]; then
  echo "CA certificates already exist."
  read -p "Regenerate CA? (y/N): " -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Regenerating CA certificates..."
    "$SCRIPT_DIR/generate-ca.sh"
  else
    echo "Using existing CA certificates."
  fi
else
  "$SCRIPT_DIR/generate-ca.sh"
fi

if [ ! -f "$INTERMEDIATE_CA_DIR/ca-intermediate.crt" ] || [ ! -f "$CERT_DIR/ca/ca-chain.crt" ]; then
  echo "Error: CA chain not found. Please run generate-ca.sh first."
  exit 1
fi

echo "✓ CA chain ready"
echo ""

# ==========================================
# Step 2: Generate User Certificates (delegate to generate-user-cert.sh)
# ==========================================
echo "Step 2: Generating user certificates..."
echo "---------------------------------------------------"

# Define users from LDAP/DB
declare -a USERS=(
  "jdoe:cn=jdoe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org"
  "asmith:cn=asmith,ou=hr,ou=users,dc=corp,dc=acme,dc=org"
  "bwilson:cn=bwilson,ou=finance,ou=users,dc=corp,dc=acme,dc=org"
  "mgarcia:cn=mgarcia,ou=it,ou=users,dc=corp,dc=acme,dc=org"
  "ktran:cn=ktran,ou=security,ou=users,dc=corp,dc=acme,dc=org"
)

for user_entry in "${USERS[@]}"; do
  IFS=':' read -r user_id user_dn <<< "$user_entry"
  if [ ! -f "$USER_CERT_DIR/${user_id}.p12" ]; then
    echo "  Generating certificate for $user_id..."
    "$SCRIPT_DIR/generate-user-cert.sh" "$user_dn" "$user_id" > /dev/null 2>&1
  else
    echo "  Certificate for $user_id already exists (skipping)"
  fi
done

echo "✓ User certificates ready"
echo ""

# ==========================================
# Step 3: Generate Server Certificates
# ==========================================
echo "Step 3: Generating server certificates (signed by Intermediate CA)..."
echo "---------------------------------------------------"

# Function to generate server certificate signed by Intermediate CA
generate_server_cert() {
  local alias=$1
  local cn=$2
  local keystore_path=$3
  local cert_path=$4

  echo "  Generating $alias certificate..."

  # Generate private key
  openssl genrsa -out "$SERVER_CERT_DIR/${alias}.key" 2048

  # Generate CSR
  openssl req -new \
    -key "$SERVER_CERT_DIR/${alias}.key" \
    -out "$SERVER_CERT_DIR/${alias}.csr" \
    -subj "/CN=$cn/O=Acme Corp/C=US"

  # Sign with Intermediate CA
  if [ "$alias" = "auth-service" ]; then
    # Add SAN for auth-service
    openssl x509 -req -days "$VALIDITY_DAYS" \
      -in "$SERVER_CERT_DIR/${alias}.csr" \
      -CA "$INTERMEDIATE_CA_DIR/ca-intermediate.crt" \
      -CAkey "$INTERMEDIATE_CA_DIR/ca-intermediate.key" \
      -CAcreateserial \
      -out "$SERVER_CERT_DIR/${alias}.crt" \
      -extensions v3_server \
      -extfile <(cat <<EOF
[v3_server]
basicConstraints = CA:FALSE
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth, clientAuth
subjectAltName = DNS:localhost,IP:127.0.0.1
EOF
)
  else
    # Regular server cert
    openssl x509 -req -days "$VALIDITY_DAYS" \
      -in "$SERVER_CERT_DIR/${alias}.csr" \
      -CA "$INTERMEDIATE_CA_DIR/ca-intermediate.crt" \
      -CAkey "$INTERMEDIATE_CA_DIR/ca-intermediate.key" \
      -CAcreateserial \
      -out "$SERVER_CERT_DIR/${alias}.crt" \
      -extensions v3_server \
      -extfile <(cat <<EOF
[v3_server]
basicConstraints = CA:FALSE
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth, clientAuth
EOF
)
  fi

  # Convert to PKCS12
  openssl pkcs12 -export \
    -out "$SERVER_CERT_DIR/${alias}.p12" \
    -inkey "$SERVER_CERT_DIR/${alias}.key" \
    -in "$SERVER_CERT_DIR/${alias}.crt" \
    -certfile "$CERT_DIR/ca/ca-chain.crt" \
    -name "$alias" \
    -passout pass:"$KEYSTORE_PASSWORD"

  # Convert PKCS12 to JKS using keytool
  keytool -importkeystore \
    -srckeystore "$SERVER_CERT_DIR/${alias}.p12" \
    -srcstoretype PKCS12 \
    -srcstorepass "$KEYSTORE_PASSWORD" \
    -destkeystore "$keystore_path" \
    -deststoretype JKS \
    -deststorepass "$KEYSTORE_PASSWORD" \
    -noprompt

  # Clean up temporary files
  rm -f "$SERVER_CERT_DIR/${alias}.key" \
        "$SERVER_CERT_DIR/${alias}.csr" \
        "$SERVER_CERT_DIR/${alias}.crt" \
        "$SERVER_CERT_DIR/${alias}.p12"

  echo "    ✓ $alias keystore created"
}

# Generate server certificates
generate_server_cert "auth-service" "auth-service" \
  "$SERVER_CERT_DIR/auth-service-keystore.jks" \
  "$SERVER_CERT_DIR/auth-service.crt"

generate_server_cert "api-mvc" "api-mvc" \
  "$SERVER_CERT_DIR/api-mvc-keystore.jks" \
  "$SERVER_CERT_DIR/api-mvc.crt"

generate_server_cert "api-webflux" "api-webflux" \
  "$SERVER_CERT_DIR/api-webflux-keystore.jks" \
  "$SERVER_CERT_DIR/api-webflux.crt"

echo "✓ Server certificates ready"
echo ""

# ==========================================
# Step 4: Create Truststore
# ==========================================
echo "Step 4: Creating truststore with CA chain..."
echo "---------------------------------------------------"

# Create truststore with Root CA and Intermediate CA
if [ -f "$SERVER_CERT_DIR/acme-truststore.jks" ]; then
  rm -f "$SERVER_CERT_DIR/acme-truststore.jks"
fi

# Import Root CA
keytool -importcert \
  -alias "root-ca" \
  -file "$ROOT_CA_DIR/ca-root.crt" \
  -keystore "$SERVER_CERT_DIR/acme-truststore.jks" \
  -storepass "$TRUSTSTORE_PASSWORD" \
  -noprompt

# Import Intermediate CA
keytool -importcert \
  -alias "intermediate-ca" \
  -file "$INTERMEDIATE_CA_DIR/ca-intermediate.crt" \
  -keystore "$SERVER_CERT_DIR/acme-truststore.jks" \
  -storepass "$TRUSTSTORE_PASSWORD" \
  -noprompt

echo "✓ Truststore created"
echo ""

# ==========================================
# Step 5: Generate Prometheus Client Certificates
# ==========================================
echo "Step 5: Generating Prometheus client certificates..."
echo "---------------------------------------------------"

# Prometheus needs PEM format certificates extracted from the API keystore
if [ -f "$SERVER_CERT_DIR/api-mvc-keystore.jks" ]; then
  # Export certificate in PEM format
  keytool -exportcert \
    -alias "api-mvc" \
    -keystore "$SERVER_CERT_DIR/api-mvc-keystore.jks" \
    -storepass "$KEYSTORE_PASSWORD" \
    -rfc \
    -file "$SERVER_CERT_DIR/client-cert.pem" \
    > /dev/null 2>&1

  # Export private key in PEM format (via temporary PKCS12)
  TEMP_P12="$SERVER_CERT_DIR/temp-prometheus.p12"
  keytool -importkeystore \
    -srckeystore "$SERVER_CERT_DIR/api-mvc-keystore.jks" \
    -destkeystore "$TEMP_P12" \
    -srcstoretype JKS \
    -deststoretype PKCS12 \
    -srcstorepass "$KEYSTORE_PASSWORD" \
    -deststorepass "$KEYSTORE_PASSWORD" \
    -srcalias "api-mvc" \
    -noprompt \
    > /dev/null 2>&1

  openssl pkcs12 \
    -in "$TEMP_P12" \
    -nodes \
    -nocerts \
    -out "$SERVER_CERT_DIR/client-key.pem" \
    -password "pass:$KEYSTORE_PASSWORD" \
    > /dev/null 2>&1

  rm -f "$TEMP_P12"
  echo "✓ Prometheus certificates ready"
else
  echo "⚠ Skipping Prometheus certificates (api-mvc keystore not found)"
fi
echo ""

# ==========================================
# Step 6: Copy Certificates to Resources
# ==========================================
echo "Step 6: Copying certificates to resource directories..."
echo "---------------------------------------------------"

# Copy auth service certificates
if [ -d "$PROJECT_ROOT/acme-auth-service-ldap/src/main/resources/ssl" ]; then
  cp "$SERVER_CERT_DIR/auth-service-keystore.jks" \
     "$PROJECT_ROOT/acme-auth-service-ldap/src/main/resources/ssl/"
  cp "$SERVER_CERT_DIR/acme-truststore.jks" \
     "$PROJECT_ROOT/acme-auth-service-ldap/src/main/resources/ssl/"
  echo "  ✓ Copied to acme-auth-service-ldap"
fi

if [ -d "$PROJECT_ROOT/acme-auth-service-db/src/main/resources/ssl" ]; then
  cp "$SERVER_CERT_DIR/auth-service-keystore.jks" \
     "$PROJECT_ROOT/acme-auth-service-db/src/main/resources/ssl/"
  cp "$SERVER_CERT_DIR/acme-truststore.jks" \
     "$PROJECT_ROOT/acme-auth-service-db/src/main/resources/ssl/"
  echo "  ✓ Copied to acme-auth-service-db"
fi

# Copy API certificates
if [ -d "$PROJECT_ROOT/acme-api-mvc/src/main/resources/ssl" ]; then
  cp "$SERVER_CERT_DIR/api-mvc-keystore.jks" \
     "$PROJECT_ROOT/acme-api-mvc/src/main/resources/ssl/"
  cp "$SERVER_CERT_DIR/acme-truststore.jks" \
     "$PROJECT_ROOT/acme-api-mvc/src/main/resources/ssl/"
  echo "  ✓ Copied to acme-api-mvc"
fi

if [ -d "$PROJECT_ROOT/acme-api-webflux/src/main/resources/ssl" ]; then
  cp "$SERVER_CERT_DIR/api-webflux-keystore.jks" \
     "$PROJECT_ROOT/acme-api-webflux/src/main/resources/ssl/"
  cp "$SERVER_CERT_DIR/acme-truststore.jks" \
     "$PROJECT_ROOT/acme-api-webflux/src/main/resources/ssl/"
  echo "  ✓ Copied to acme-api-webflux"
fi

echo "✓ Certificates copied to resource directories"
echo ""

# ==========================================
# Summary
# ==========================================
echo "=========================================="
echo "Certificate Setup Complete!"
echo "=========================================="
echo ""
echo "Generated certificates:"
echo ""
echo "CA Chain:"
echo "  - $ROOT_CA_DIR/ca-root.crt"
echo "  - $INTERMEDIATE_CA_DIR/ca-intermediate.crt"
echo "  - $CERT_DIR/ca/ca-chain.crt"
echo ""
echo "User Certificates (for browser):"
for user_entry in "${USERS[@]}"; do
  IFS=':' read -r user_id user_dn <<< "$user_entry"
  echo "  - $USER_CERT_DIR/${user_id}.p12"
done
echo ""
echo "Server Certificates:"
echo "  - $SERVER_CERT_DIR/auth-service-keystore.jks"
echo "  - $SERVER_CERT_DIR/api-mvc-keystore.jks"
echo "  - $SERVER_CERT_DIR/api-webflux-keystore.jks"
echo "  - $SERVER_CERT_DIR/acme-truststore.jks"
echo ""
echo "Prometheus Certificates:"
echo "  - $SERVER_CERT_DIR/client-cert.pem"
echo "  - $SERVER_CERT_DIR/client-key.pem"
echo ""
echo "Next steps:"
echo "  1. Import Root CA into browser: $ROOT_CA_DIR/ca-root.crt"
echo "  2. Import user certificates into browser: $USER_CERT_DIR/*.p12"
echo "  3. Start the auth service - it will use HTTPS with the generated certificates"
echo ""
echo "All certificates share the same CA trust chain!"
echo ""
echo "Note: Old scripts (generate-mtls-certs.sh, copy-mtls-certs.sh) are"
echo "      no longer needed. Use this script for all certificate setup."
echo ""
