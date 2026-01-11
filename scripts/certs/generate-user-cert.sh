#!/bin/bash
# Generate X509 certificate for LDAP user
# Usage: ./generate-user-cert.sh "cn=jdoe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org" jdoe
#
# The DN format uses CN as the RDN with the UID value (e.g., "cn=jdoe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org")
# This matches the LDAP DN format exactly, where CN in the DN is the UID, but the CN attribute is the full name.

set -e

USER_DN="$1"
USER_ID="$2"  # e.g., jdoe

if [ -z "$USER_DN" ] || [ -z "$USER_ID" ]; then
  echo "Usage: $0 <user_dn> <user_id>"
  echo "Example: $0 'cn=jdoe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org' jdoe"
  echo ""
  echo "Note: The DN should use CN as the RDN with the UID value to match LDAP DN format."
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CERT_DIR="$PROJECT_ROOT/k8s/certs"
CA_INTERMEDIATE="$CERT_DIR/ca/intermediate"
CA_CHAIN="$CERT_DIR/ca/ca-chain.crt"
USER_CERT_DIR="$CERT_DIR/users"

# Check if CA exists
if [ ! -f "$CA_INTERMEDIATE/ca-intermediate.crt" ]; then
  echo "Error: Intermediate CA not found. Please generate the CA first."
  echo "See docs/K8S.md Phase 1 for CA setup instructions."
  exit 1
fi

if [ ! -f "$CA_CHAIN" ]; then
  echo "Error: CA chain not found. Please generate the CA chain first."
  exit 1
fi

mkdir -p "$USER_CERT_DIR"

echo "Generating certificate for user: $USER_ID"
echo "DN: $USER_DN"

# Generate user private key
openssl genrsa -out "$USER_CERT_DIR/${USER_ID}.key" 2048

# Convert DN to OpenSSL -subj format
# OpenSSL -subj format: /CN=value/OU=value1/OU=value2/DC=value1/DC=value2
# Note: OpenSSL may not fully support DC in subject, but we'll include it for LDAP compatibility

# Convert DN components to OpenSSL format
OPENSSL_SUBJ=""
IFS=',' read -ra DN_PARTS <<< "$USER_DN"
for part in "${DN_PARTS[@]}"; do
  if [[ $part =~ ^cn=(.+)$ ]]; then
    OPENSSL_SUBJ="${OPENSSL_SUBJ}/CN=${BASH_REMATCH[1]}"
  elif [[ $part =~ ^ou=(.+)$ ]]; then
    # Capitalize first letter of OU value
    OU_VALUE="${BASH_REMATCH[1]^}"
    OPENSSL_SUBJ="${OPENSSL_SUBJ}/OU=${OU_VALUE}"
  elif [[ $part =~ ^dc=(.+)$ ]]; then
    # OpenSSL may not support DC, but we'll try it
    OPENSSL_SUBJ="${OPENSSL_SUBJ}/DC=${BASH_REMATCH[1]}"
  fi
done

# Create certificate config for extensions only
cat > "$USER_CERT_DIR/${USER_ID}.conf" <<EOF
[v3_req]
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = clientAuth
EOF

# Generate certificate signing request using -subj
openssl req -new -key "$USER_CERT_DIR/${USER_ID}.key" \
  -out "$USER_CERT_DIR/${USER_ID}.csr" \
  -subj "$OPENSSL_SUBJ" \
  -config "$USER_CERT_DIR/${USER_ID}.conf"

# Sign certificate with intermediate CA (valid for 1 year)
openssl x509 -req -days 365 \
  -in "$USER_CERT_DIR/${USER_ID}.csr" \
  -CA "$CA_INTERMEDIATE/ca-intermediate.crt" \
  -CAkey "$CA_INTERMEDIATE/ca-intermediate.key" \
  -CAcreateserial \
  -out "$USER_CERT_DIR/${USER_ID}.crt" \
  -extensions v3_req \
  -extfile "$USER_CERT_DIR/${USER_ID}.conf"

# Create PKCS#12 bundle for browser import (no password for easier import)
openssl pkcs12 -export \
  -out "$USER_CERT_DIR/${USER_ID}.p12" \
  -inkey "$USER_CERT_DIR/${USER_ID}.key" \
  -in "$USER_CERT_DIR/${USER_ID}.crt" \
  -certfile "$CA_CHAIN" \
  -passout pass:  # No password for easier import

# Clean up temporary files
rm -f "$USER_CERT_DIR/${USER_ID}.conf" "$USER_CERT_DIR/${USER_ID}.csr"

echo ""
echo "✓ Certificate generated successfully!"
echo "  Certificate: $USER_CERT_DIR/${USER_ID}.p12"
echo "  Private key: $USER_CERT_DIR/${USER_ID}.key"
echo "  Certificate: $USER_CERT_DIR/${USER_ID}.crt"
echo ""
echo "Import $USER_CERT_DIR/${USER_ID}.p12 into your browser's certificate store."
echo "See docs/K8S.md for browser import instructions."
