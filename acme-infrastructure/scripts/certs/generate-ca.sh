#!/bin/bash
# Generate Root CA and Intermediate CA for X509 client certificate authentication
# This script creates the certificate authority chain needed to sign user certificates

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
CERT_DIR="$PROJECT_ROOT/acme-infrastructure/certs"
ROOT_CA_DIR="$CERT_DIR/ca/root"
INTERMEDIATE_CA_DIR="$CERT_DIR/ca/intermediate"

echo "Generating Certificate Authority (CA) chain..."
echo "Certificate directory: $CERT_DIR"
echo ""

# Create directory structure
mkdir -p "$ROOT_CA_DIR" "$INTERMEDIATE_CA_DIR"

# Check if CA already exists
if [ -f "$ROOT_CA_DIR/ca-root.crt" ] || [ -f "$INTERMEDIATE_CA_DIR/ca-intermediate.crt" ]; then
  echo "Warning: CA certificates already exist!"
  read -p "Do you want to regenerate them? (y/N): " -n 1 -r
  echo
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Aborted. Using existing CA certificates."
    exit 0
  fi
  echo "Regenerating CA certificates..."
fi

# 1. Generate Root CA
echo "1. Generating Root CA..."
openssl genrsa -out "$ROOT_CA_DIR/ca-root.key" 4096

# Create root CA config file
cat > "$ROOT_CA_DIR/ca-root.conf" <<EOF
[req]
distinguished_name = req_distinguished_name
x509_extensions = v3_root_ca
prompt = no

[req_distinguished_name]
CN = Acme Root CA
O = Acme Corp
C = US

[v3_root_ca]
basicConstraints = critical,CA:true
keyUsage = critical,keyCertSign,cRLSign
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
EOF

openssl req -new -x509 -days 3650 \
  -key "$ROOT_CA_DIR/ca-root.key" \
  -out "$ROOT_CA_DIR/ca-root.crt" \
  -config "$ROOT_CA_DIR/ca-root.conf"

echo "   ✓ Root CA generated: $ROOT_CA_DIR/ca-root.crt"
echo ""

# 2. Generate Intermediate CA
echo "2. Generating Intermediate CA..."
openssl genrsa -out "$INTERMEDIATE_CA_DIR/ca-intermediate.key" 4096

openssl req -new \
  -key "$INTERMEDIATE_CA_DIR/ca-intermediate.key" \
  -out "$INTERMEDIATE_CA_DIR/ca-intermediate.csr" \
  -subj "/CN=Acme Intermediate CA/O=Acme Corp/C=US"

openssl x509 -req -days 1825 \
  -in "$INTERMEDIATE_CA_DIR/ca-intermediate.csr" \
  -CA "$ROOT_CA_DIR/ca-root.crt" \
  -CAkey "$ROOT_CA_DIR/ca-root.key" \
  -CAcreateserial \
  -out "$INTERMEDIATE_CA_DIR/ca-intermediate.crt" \
  -extensions v3_intermediate_ca \
  -extfile <(cat <<EOF
[v3_intermediate_ca]
basicConstraints = critical,CA:true,pathlen:0
keyUsage = critical,keyCertSign,cRLSign
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid:always,issuer
EOF
)

echo "   ✓ Intermediate CA generated: $INTERMEDIATE_CA_DIR/ca-intermediate.crt"
echo ""

# 3. Create CA Chain
echo "3. Creating CA certificate chain..."
cat "$INTERMEDIATE_CA_DIR/ca-intermediate.crt" \
    "$ROOT_CA_DIR/ca-root.crt" > "$CERT_DIR/ca/ca-chain.crt"

echo "   ✓ CA chain created: $CERT_DIR/ca/ca-chain.crt"
echo ""

# Clean up temporary files
rm -f "$INTERMEDIATE_CA_DIR/ca-intermediate.csr" "$ROOT_CA_DIR/ca-root.conf"

echo "✓ Certificate Authority setup complete!"
echo ""
echo "Generated files:"
echo "  Root CA:        $ROOT_CA_DIR/ca-root.crt"
echo "  Root CA Key:    $ROOT_CA_DIR/ca-root.key (KEEP SECRET!)"
echo "  Intermediate:   $INTERMEDIATE_CA_DIR/ca-intermediate.crt"
echo "  Intermediate Key: $INTERMEDIATE_CA_DIR/ca-intermediate.key (KEEP SECRET!)"
echo "  CA Chain:       $CERT_DIR/ca/ca-chain.crt"
echo ""
echo "Next steps:"
echo "  1. Import $ROOT_CA_DIR/ca-root.crt into your browser's trusted root store"
echo "  2. Generate user certificates using: acme-infrastructure/scripts/certs/generate-user-cert.sh"
echo ""
