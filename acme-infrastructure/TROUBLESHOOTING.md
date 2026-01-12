# Troubleshooting Minikube Ingress

## Issue: Page Never Loads in Chrome

If the page never loads (hangs, doesn't prompt for certificate, or shows errors):

### Check 1: Port-Forward is Running

```bash
ps aux | grep "kubectl port-forward.*8443"
```

If not running, start it:

```bash
./acme-infrastructure/scripts/port-forward.sh
```

### Check 2: Client Certificate Required

The Ingress requires a client certificate. Chrome should prompt you. If it doesn't:

- Check that Root CA is imported and trusted
- Check that user certificate is imported
- Try accessing: `https://acme.local:8443/test` (with port if using port-forward)

### Check 3: TLS Certificate Issues

The Ingress might be using a fake certificate. Check:

```bash
kubectl get secret ingress-tls-secret -n acme-apps
```

If missing, create it (see Problem 1 below).

### Check 4: Browser Console Errors

Open Chrome DevTools (F12) → Console tab and check for errors.

### Check 5: Chrome Hangs / Spins Forever

If Chrome tab spins forever without loading:

**Issue**: Chrome is likely waiting for a client certificate but not being prompted, or the TLS handshake is hanging.

**Possible Causes**:

1. **CA Chain Secret Format**: NGINX Ingress expects the CA certificate with key name `ca.crt` (not `ca-chain.crt`)

   ```bash
   # Verify secret format
   kubectl get secret ca-chain-secret -n acme-apps -o jsonpath='{.data}' | grep -o '"[^"]*"'
   # Should show: "ca.crt"
   
   # If wrong, recreate:
   kubectl delete secret ca-chain-secret -n acme-apps
   kubectl create secret generic ca-chain-secret \
     --from-file=ca.crt=acme-infrastructure/certs/ca/ca-chain.crt \
     -n acme-apps
   ```

2. **Chrome Not Prompting for Certificate**: 
   - **Important**: Chrome on macOS uses the **login** keychain for client certificates, NOT System keychain
   - Check where certificate is: `security find-certificate -c "jdoe" -a | grep "keychain:"`
   - If in System keychain, remove it and import to login keychain:

     ```bash
     # Remove from System keychain
     sudo security delete-certificate -c "jdoe" /Library/Keychains/System.keychain
     
     # Import to login keychain (Chrome will see it)
     security import acme-infrastructure/certs/users/jdoe.p12 -k ~/Library/Keychains/login.keychain-db
     ```

   - Or import directly into Chrome: `chrome://settings/certificates` → Your certificates → Import
   - Verify Root CA is trusted: `security find-certificate -c "Acme Root CA"`

3. **TLS Handshake Hanging**:
   - The Ingress might be waiting for client cert but Chrome isn't sending it
   - Check Ingress logs: `kubectl logs -n ingress-nginx -l app.kubernetes.io/component=controller --tail=50`
   - Look for SSL/TLS errors or client certificate messages

**Quick Test**: Try accessing with curl to see if it prompts or hangs:

```bash
curl -k -v --max-time 5 https://localhost:8443/test -H "Host: acme.local"
```

If curl also hangs, the Ingress isn't properly configured for client certificates.

## Issue: Nothing happens in Chrome (Old)

### Problem 1: Missing TLS Secret

The Ingress requires a TLS secret for HTTPS. Check if it exists:

```bash
kubectl get secret ingress-tls-secret -n acme-ingress
```

If it doesn't exist, create it:

```bash
# First, ensure certificates are generated
./acme-infrastructure/scripts/certs/setup-all-certs.sh

# Then create the TLS secret
kubectl create secret tls ingress-tls-secret \
  --cert=monitoring/certs/auth-service.crt \
  --key=monitoring/certs/auth-service.key \
  -n acme-ingress
```

### Problem 2: Ingress Has No External IP / Tunnel Stuck

Minikube requires `minikube tunnel` to expose the Ingress, but it may get stuck waiting for sudo.

**Solution A: Change Service Type to LoadBalancer**

```bash
# Change Ingress controller to LoadBalancer
kubectl patch svc ingress-nginx-controller -n ingress-nginx -p '{"spec":{"type":"LoadBalancer"}}'

# Check if service gets EXTERNAL-IP
kubectl get svc ingress-nginx-controller -n ingress-nginx

# If EXTERNAL-IP is 127.0.0.1, add to /etc/hosts
echo "127.0.0.1 acme.local" | sudo tee -a /etc/hosts
```

**Solution B: Use Minikube Tunnel (if sudo works)**

Run this in a **separate terminal** (keep it running):

```bash
minikube tunnel
```

After starting the tunnel, wait a few seconds and check:

```bash
kubectl get ingress -n acme-apps
```

**Note**: The Ingress might not show an ADDRESS even if the service is accessible. Check the service EXTERNAL-IP instead.

### Problem 3: /etc/hosts Configuration

Make sure `/etc/hosts` points to the Minikube IP:

```bash
# Get Minikube IP
minikube ip

# Add to /etc/hosts (replace with actual IP)
echo "$(minikube ip) acme.local" | sudo tee -a /etc/hosts
```

### Problem 4: Browser Certificate Issues

1. **Root CA not trusted**: Import the Root CA into your browser (see `docs/X509.md`)
2. **User certificate not imported**: Import your user certificate (see `docs/X509.md`)
3. **Certificate not prompted**: Check browser console for errors

### Problem 5: Connection Refused

Check if services are running:

```bash
# Check Ingress controller
kubectl get pods -n ingress-nginx

# Check test service
kubectl get pods -n acme-apps

# Check Ingress status
kubectl describe ingress acme-ingress -n acme-apps
```

### Problem 6: HTTPS Not Working

1. Verify TLS secret exists and is correct:

   ```bash
   kubectl get secret ingress-tls-secret -n acme-ingress -o yaml
   ```

2. Check Ingress TLS configuration:

   ```bash
   kubectl get ingress acme-ingress -n acme-apps -o yaml | grep -A 5 tls
   ```

3. Test with curl (bypass certificate validation):

   ```bash
   curl -k https://acme.local/test
   ```

## Quick Diagnostic Commands

```bash
# Check all components
kubectl get ingress -n acme-apps
kubectl get svc -n acme-apps
kubectl get pods -n acme-apps
kubectl get secret ingress-tls-secret -n acme-ingress

# Check Minikube tunnel
ps aux | grep "minikube tunnel"

# Test connectivity
curl -k -v https://acme.local/test
```
