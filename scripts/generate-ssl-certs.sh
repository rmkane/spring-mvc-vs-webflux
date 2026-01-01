#!/usr/bin/env bash

# Generate SSL certificates for mutual TLS (mTLS)
# This script creates:
# 1. A keystore for the auth service (server certificate)
# 2. A keystore for the MVC API (client certificate)
# 3. A keystore for the WebFlux API (client certificate)
# 4. A shared truststore containing all certificates (for both server and clients)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CERT_DIR="$PROJECT_DIR/monitoring/certs"
KEYSTORE_PASSWORD="changeit"
TRUSTSTORE_PASSWORD="changeit"
VALIDITY_DAYS=365

echo "Generating SSL certificates for mutual TLS (mTLS)..."
echo "Certificate directory: $CERT_DIR"
echo ""

# Create certs directory if it doesn't exist
mkdir -p "$CERT_DIR"

# Generate server keystore (for auth service)
echo "1. Generating server keystore (auth-service)..."
keytool -genkeypair \
  -alias "auth-service" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$VALIDITY_DAYS" \
  -storetype JKS \
  -keystore "$CERT_DIR/auth-service-keystore.jks" \
  -storepass "$KEYSTORE_PASSWORD" \
  -keypass "$KEYSTORE_PASSWORD" \
  -dname "CN=auth-service, OU=Development, O=Acme, L=City, ST=State, C=US" \
  -ext "SAN=DNS:localhost,IP:127.0.0.1"

echo "   Server keystore created: $CERT_DIR/auth-service-keystore.jks"
echo ""

# Export server certificate
echo "2. Exporting server certificate..."
keytool -exportcert \
  -alias "auth-service" \
  -keystore "$CERT_DIR/auth-service-keystore.jks" \
  -storepass "$KEYSTORE_PASSWORD" \
  -file "$CERT_DIR/auth-service-cert.cer"

echo "   Server certificate exported: $CERT_DIR/auth-service-cert.cer"
echo ""

# Generate client keystore for MVC API
echo "3. Generating client keystore (api-mvc)..."
keytool -genkeypair \
  -alias "api-mvc" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$VALIDITY_DAYS" \
  -storetype JKS \
  -keystore "$CERT_DIR/api-mvc-keystore.jks" \
  -storepass "$KEYSTORE_PASSWORD" \
  -keypass "$KEYSTORE_PASSWORD" \
  -dname "CN=api-mvc, OU=Development, O=Acme, L=City, ST=State, C=US"

echo "   Client keystore created: $CERT_DIR/api-mvc-keystore.jks"
echo ""

# Export MVC client certificate
echo "4. Exporting MVC client certificate..."
keytool -exportcert \
  -alias "api-mvc" \
  -keystore "$CERT_DIR/api-mvc-keystore.jks" \
  -storepass "$KEYSTORE_PASSWORD" \
  -file "$CERT_DIR/api-mvc-cert.cer"

echo "   MVC client certificate exported: $CERT_DIR/api-mvc-cert.cer"
echo ""

# Generate client keystore for WebFlux API
echo "5. Generating client keystore (api-webflux)..."
keytool -genkeypair \
  -alias "api-webflux" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "$VALIDITY_DAYS" \
  -storetype JKS \
  -keystore "$CERT_DIR/api-webflux-keystore.jks" \
  -storepass "$KEYSTORE_PASSWORD" \
  -keypass "$KEYSTORE_PASSWORD" \
  -dname "CN=api-webflux, OU=Development, O=Acme, L=City, ST=State, C=US"

echo "   Client keystore created: $CERT_DIR/api-webflux-keystore.jks"
echo ""

# Export WebFlux client certificate
echo "6. Exporting WebFlux client certificate..."
keytool -exportcert \
  -alias "api-webflux" \
  -keystore "$CERT_DIR/api-webflux-keystore.jks" \
  -storepass "$KEYSTORE_PASSWORD" \
  -file "$CERT_DIR/api-webflux-cert.cer"

echo "   WebFlux client certificate exported: $CERT_DIR/api-webflux-cert.cer"
echo ""

# Create shared truststore (contains all certificates - server and clients)
echo "7. Creating shared truststore (contains all certificates)..."
# Start with server certificate
keytool -importcert \
  -alias "auth-service" \
  -file "$CERT_DIR/auth-service-cert.cer" \
  -keystore "$CERT_DIR/auth-service-truststore.jks" \
  -storepass "$TRUSTSTORE_PASSWORD" \
  -noprompt

# Add MVC client certificate
keytool -importcert \
  -alias "api-mvc" \
  -file "$CERT_DIR/api-mvc-cert.cer" \
  -keystore "$CERT_DIR/auth-service-truststore.jks" \
  -storepass "$TRUSTSTORE_PASSWORD" \
  -noprompt

# Add WebFlux client certificate
keytool -importcert \
  -alias "api-webflux" \
  -file "$CERT_DIR/api-webflux-cert.cer" \
  -keystore "$CERT_DIR/auth-service-truststore.jks" \
  -storepass "$TRUSTSTORE_PASSWORD" \
  -noprompt

echo "   Shared truststore created: $CERT_DIR/auth-service-truststore.jks"
echo ""

# Clean up temporary certificate files
rm -f "$CERT_DIR/auth-service-cert.cer"
rm -f "$CERT_DIR/api-mvc-cert.cer"
rm -f "$CERT_DIR/api-webflux-cert.cer"

echo "SSL certificate generation complete!"
echo ""
echo "Files created:"
echo "  - $CERT_DIR/auth-service-keystore.jks (server keystore, password: $KEYSTORE_PASSWORD)"
echo "  - $CERT_DIR/api-mvc-keystore.jks (MVC client keystore, password: $KEYSTORE_PASSWORD)"
echo "  - $CERT_DIR/api-webflux-keystore.jks (WebFlux client keystore, password: $KEYSTORE_PASSWORD)"
echo "  - $CERT_DIR/auth-service-truststore.jks (shared truststore, password: $TRUSTSTORE_PASSWORD)"
echo ""
echo "Note: These are self-signed certificates for development only."
echo "      For production, use certificates from a trusted CA."

