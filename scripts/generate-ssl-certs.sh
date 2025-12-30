#!/bin/bash

# Generate SSL certificates for the auth service
# This script creates:
# 1. A keystore for the auth service (server certificate)
# 2. A truststore for the clients (to trust the server certificate)

set -e

CERT_DIR="$(cd "$(dirname "$0")/.." && pwd)/certs"
KEYSTORE_PASSWORD="changeit"
TRUSTSTORE_PASSWORD="changeit"
KEY_ALIAS="auth-service"
CN="localhost"
VALIDITY_DAYS=365

echo "Generating SSL certificates for auth service..."
echo "Certificate directory: $CERT_DIR"
echo ""

# Create certs directory if it doesn't exist
mkdir -p "$CERT_DIR"

# Generate server keystore (for auth service)
echo "1. Generating server keystore..."
keytool -genkeypair \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$VALIDITY_DAYS" \
  -storetype JKS \
  -keystore "$CERT_DIR/auth-service-keystore.jks" \
  -storepass "$KEYSTORE_PASSWORD" \
  -keypass "$KEYSTORE_PASSWORD" \
  -dname "CN=$CN, OU=Development, O=Acme, L=City, ST=State, C=US" \
  -ext "SAN=DNS:localhost,IP:127.0.0.1"

echo "   Server keystore created: $CERT_DIR/auth-service-keystore.jks"
echo ""

# Export server certificate
echo "2. Exporting server certificate..."
keytool -exportcert \
  -alias "$KEY_ALIAS" \
  -keystore "$CERT_DIR/auth-service-keystore.jks" \
  -storepass "$KEYSTORE_PASSWORD" \
  -file "$CERT_DIR/auth-service-cert.cer"

echo "   Server certificate exported: $CERT_DIR/auth-service-cert.cer"
echo ""

# Create client truststore (for MVC and WebFlux APIs)
echo "3. Creating client truststore..."
keytool -importcert \
  -alias "$KEY_ALIAS" \
  -file "$CERT_DIR/auth-service-cert.cer" \
  -keystore "$CERT_DIR/auth-service-truststore.jks" \
  -storepass "$TRUSTSTORE_PASSWORD" \
  -noprompt

echo "   Client truststore created: $CERT_DIR/auth-service-truststore.jks"
echo ""

# Clean up temporary certificate file
rm -f "$CERT_DIR/auth-service-cert.cer"

echo "SSL certificate generation complete!"
echo ""
echo "Files created:"
echo "  - $CERT_DIR/auth-service-keystore.jks (server keystore, password: $KEYSTORE_PASSWORD)"
echo "  - $CERT_DIR/auth-service-truststore.jks (client truststore, password: $TRUSTSTORE_PASSWORD)"
echo ""
echo "Note: These are self-signed certificates for development only."
echo "      For production, use certificates from a trusted CA."

