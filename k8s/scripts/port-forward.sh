#!/bin/bash
# Port-forward script for testing Ingress locally
# This avoids the need for minikube tunnel and sudo

echo "Starting port-forward for Ingress controller..."
echo "Access via: https://localhost:8443/test"
echo ""
echo "Make sure /etc/hosts has:"
echo "  127.0.0.1 acme.local"
echo ""
echo "Press Ctrl+C to stop"
echo ""

kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8443:443
