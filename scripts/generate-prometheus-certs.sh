#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CERTS_DIR="$PROJECT_DIR/monitoring/certs"
KEYSTORE_PATH="$PROJECT_DIR/certs/api-mvc-keystore.jks"
KEYSTORE_PASSWORD="changeit"
KEYSTORE_ALIAS="api-mvc"

echo "Generating Prometheus client certificates for auth-service mTLS..."

# Create certs directory if it doesn't exist
mkdir -p "$CERTS_DIR"

# Clean up old certificates
rm -f "$CERTS_DIR/client-cert.pem" "$CERTS_DIR/client-key.pem"

# Export certificate in PEM format
echo "Exporting client certificate from keystore..."
keytool -exportcert \
    -alias "$KEYSTORE_ALIAS" \
    -keystore "$KEYSTORE_PATH" \
    -storepass "$KEYSTORE_PASSWORD" \
    -rfc \
    -file "$CERTS_DIR/client-cert.pem"

# Export private key in PEM format
echo "Exporting client private key from keystore..."
TEMP_P12="/tmp/prometheus-temp-$$.p12"
keytool -importkeystore \
    -srckeystore "$KEYSTORE_PATH" \
    -destkeystore "$TEMP_P12" \
    -srcstoretype JKS \
    -deststoretype PKCS12 \
    -srcstorepass "$KEYSTORE_PASSWORD" \
    -deststorepass "$KEYSTORE_PASSWORD" \
    -srcalias "$KEYSTORE_ALIAS" \
    -noprompt

openssl pkcs12 \
    -in "$TEMP_P12" \
    -nodes \
    -nocerts \
    -out "$CERTS_DIR/client-key.pem" \
    -password "pass:$KEYSTORE_PASSWORD"

rm -f "$TEMP_P12"

echo "Certificates generated successfully:"
echo "  - $CERTS_DIR/client-cert.pem"
echo "  - $CERTS_DIR/client-key.pem"
echo ""
echo "To apply changes, restart Prometheus:"
echo "  docker-compose restart prometheus"
