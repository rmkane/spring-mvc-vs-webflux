#!/usr/bin/env bash

# Copy generated SSL certificates from the certs directory to the appropriate
# service resource directories for mTLS configuration.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CERTS_DIR="$PROJECT_DIR/certs"

# Service resource directories
AUTH_SERVICE_SSL="$PROJECT_DIR/acme-auth-service/src/main/resources/ssl"
API_MVC_SSL="$PROJECT_DIR/acme-api-mvc/src/main/resources/ssl"
API_WEBFLUX_SSL="$PROJECT_DIR/acme-api-webflux/src/main/resources/ssl"

echo "Copying SSL certificates to service resource directories..."
echo "Source: $CERTS_DIR"
echo ""

# Verify source certificates exist
if [ ! -f "$CERTS_DIR/auth-service-keystore.jks" ]; then
    echo "Error: Source certificates not found in $CERTS_DIR"
    echo "Please run ./scripts/generate-ssl-certs.sh first"
    exit 1
fi

# Create SSL directories if they don't exist
mkdir -p "$AUTH_SERVICE_SSL"
mkdir -p "$API_MVC_SSL"
mkdir -p "$API_WEBFLUX_SSL"

# Copy auth-service certificates
echo "1. Copying auth-service certificates..."
cp -v "$CERTS_DIR/auth-service-keystore.jks" "$AUTH_SERVICE_SSL/"
cp -v "$CERTS_DIR/auth-service-truststore.jks" "$AUTH_SERVICE_SSL/"
echo ""

# Copy api-mvc certificates
echo "2. Copying api-mvc certificates..."
cp -v "$CERTS_DIR/api-mvc-keystore.jks" "$API_MVC_SSL/"
cp -v "$CERTS_DIR/auth-service-truststore.jks" "$API_MVC_SSL/"
echo ""

# Copy api-webflux certificates
echo "3. Copying api-webflux certificates..."
cp -v "$CERTS_DIR/api-webflux-keystore.jks" "$API_WEBFLUX_SSL/"
cp -v "$CERTS_DIR/auth-service-truststore.jks" "$API_WEBFLUX_SSL/"
echo ""

echo "SSL certificates copied successfully!"
echo ""
echo "Certificates copied to:"
echo "  Auth Service:"
echo "    - $AUTH_SERVICE_SSL/auth-service-keystore.jks"
echo "    - $AUTH_SERVICE_SSL/auth-service-truststore.jks"
echo ""
echo "  API MVC:"
echo "    - $API_MVC_SSL/api-mvc-keystore.jks"
echo "    - $API_MVC_SSL/auth-service-truststore.jks"
echo ""
echo "  API WebFlux:"
echo "    - $API_WEBFLUX_SSL/api-webflux-keystore.jks"
echo "    - $API_WEBFLUX_SSL/auth-service-truststore.jks"
echo ""
echo "Note: You may need to rebuild your applications for the changes to take effect."