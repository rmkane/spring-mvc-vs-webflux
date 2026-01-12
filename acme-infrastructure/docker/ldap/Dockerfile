# Custom LDAP image based on osixia/openldap with Kubernetes fix
FROM osixia/openldap:latest

# Replace /bin/hostname with a wrapper that returns "ldap"
RUN mv /bin/hostname /bin/hostname.orig && \
    echo '#!/bin/sh' > /bin/hostname && \
    echo 'echo "ldap"' >> /bin/hostname && \
    chmod +x /bin/hostname

# Completely rewrite the process.sh script to use correct URL format
RUN cat > /container/service/slapd/process.sh << 'EOF'
#!/bin/bash -e

# set -x (bash debug) if log level is trace
log-helper level eq trace && set -x

# Reduce maximum number of number of open file descriptors to 1024
ulimit -n $LDAP_NOFILE

# Kubernetes fix: Use 0.0.0.0 to bind to all interfaces
# CRITICAL: Delete ALL olcListener entries from config before starting
# slapd reads config first, so we must fix it before exec
if [ -d /etc/ldap/slapd.d ]; then
  # Remove all olcListener lines (including malformed ones with :tcp://)
  # Use more aggressive pattern matching to catch all variations
  find /etc/ldap/slapd.d -name "*.ldif" -type f -exec sed -i '/olcListener:/d' {} \; 2>/dev/null || true
  # Also remove any lines containing :tcp:// pattern (malformed URLs)
  find /etc/ldap/slapd.d -name "*.ldif" -type f -exec sed -i '/:tcp:\/\//d' {} \; 2>/dev/null || true
  # Remove any lines with FQDN patterns that might cause issues
  find /etc/ldap/slapd.d -name "*.ldif" -type f -exec sed -i '/\.svc\.cluster\.local/d' {} \; 2>/dev/null || true
fi

# Now use -h flag to set listeners via command line (this overrides config)
# Use explicit IP addresses to avoid FQDN resolution issues
HOST_PARAM="ldap://0.0.0.0:${LDAP_PORT:-389} ldaps://0.0.0.0:${LDAPS_PORT:-636} ldapi:///"
exec /usr/sbin/slapd -h "$HOST_PARAM" -F /etc/ldap/slapd.d -u openldap -g openldap -d "${LDAP_LOG_LEVEL:-256}"
EOF
RUN chmod +x /container/service/slapd/process.sh

# Patch the startup script to prevent malformed URL construction
# The startup script constructs URLs like "ldap://FQDN:tcp://IP:PORT" which is invalid
RUN if [ -f /container/service/slapd/startup.sh ]; then \
      # Replace FQDN construction with simple hostname \
      sed -i 's|FQDN="\$(/bin/hostname --fqdn)"|FQDN="ldap"|g' /container/service/slapd/startup.sh 2>/dev/null || true; \
      sed -i 's|\$(/bin/hostname --fqdn)|ldap|g' /container/service/slapd/startup.sh 2>/dev/null || true; \
      # Replace any URL construction that includes :tcp:// pattern \
      sed -i 's|ldap://[^:]*:tcp://[^:]*:[0-9]*|ldap://0.0.0.0:389|g' /container/service/slapd/startup.sh 2>/dev/null || true; \
      sed -i 's|ldaps://[^:]*:tcp://[^:]*:[0-9]*|ldaps://0.0.0.0:636|g' /container/service/slapd/startup.sh 2>/dev/null || true; \
    fi

# Create a custom entrypoint that patches config after generation
RUN echo '#!/bin/bash' > /custom-entrypoint.sh && \
    echo 'set -e' >> /custom-entrypoint.sh && \
    echo '' >> /custom-entrypoint.sh && \
    echo '# Copy custom schema and LDIF files' >> /custom-entrypoint.sh && \
    echo 'mkdir -p /container/service/slapd/assets/config/bootstrap/schema/custom' >> /custom-entrypoint.sh && \
    echo 'mkdir -p /container/service/slapd/assets/config/bootstrap/ldif/custom' >> /custom-entrypoint.sh && \
    echo 'cp /schema-import/*.schema /container/service/slapd/assets/config/bootstrap/schema/custom/ 2>/dev/null || true' >> /custom-entrypoint.sh && \
    echo 'cp /ldif-import/*.ldif /container/service/slapd/assets/config/bootstrap/ldif/custom/ 2>/dev/null || true' >> /custom-entrypoint.sh && \
    echo '' >> /custom-entrypoint.sh && \
    echo '# Set HOSTNAME env var' >> /custom-entrypoint.sh && \
    echo 'export HOSTNAME=ldap' >> /custom-entrypoint.sh && \
    echo '' >> /custom-entrypoint.sh && \
    echo '# Function to patch slapd config files using ldapmodify' >> /custom-entrypoint.sh && \
    echo 'patch_config() {' >> /custom-entrypoint.sh && \
    echo '  # Wait for slapd to be ready on ldapi://' >> /custom-entrypoint.sh && \
    echo '  sleep 2' >> /custom-entrypoint.sh && \
    echo '  # Use ldapmodify to fix the listener URLs' >> /custom-entrypoint.sh && \
    echo '  ldapmodify -Y EXTERNAL -H ldapi:/// <<EOF 2>/dev/null || true' >> /custom-entrypoint.sh && \
    echo 'dn: cn=config' >> /custom-entrypoint.sh && \
    echo 'changetype: modify' >> /custom-entrypoint.sh && \
    echo 'replace: olcListener' >> /custom-entrypoint.sh && \
    echo 'olcListener: ldap://0.0.0.0:389' >> /custom-entrypoint.sh && \
    echo 'olcListener: ldaps://0.0.0.0:636' >> /custom-entrypoint.sh && \
    echo 'olcListener: ldapi:///' >> /custom-entrypoint.sh && \
    echo 'EOF' >> /custom-entrypoint.sh && \
    echo '  # Also patch LDIF files directly as backup' >> /custom-entrypoint.sh && \
    echo '  if [ -d /etc/ldap/slapd.d ]; then' >> /custom-entrypoint.sh && \
    echo '    find /etc/ldap/slapd.d -name "*.ldif" -exec sed -i "s|ldap://[^ ]*:tcp://[^ ]*|ldap://0.0.0.0:389|g" {} \; 2>/dev/null || true' >> /custom-entrypoint.sh && \
    echo '    find /etc/ldap/slapd.d -name "*.ldif" -exec sed -i "s|ldaps://[^ ]*:tcp://[^ ]*|ldaps://0.0.0.0:636|g" {} \; 2>/dev/null || true' >> /custom-entrypoint.sh && \
    echo '    find /etc/ldap/slapd.d -name "*.ldif" -exec sed -i "s|:tcp://[^ ]*||g" {} \; 2>/dev/null || true' >> /custom-entrypoint.sh && \
    echo '  fi' >> /custom-entrypoint.sh && \
    echo '}' >> /custom-entrypoint.sh && \
    echo '' >> /custom-entrypoint.sh && \
    echo '# Start slapd temporarily to generate config, then patch it with Python' >> /custom-entrypoint.sh && \
    echo '/container/tool/run --wait-first-startup &' >> /custom-entrypoint.sh && \
    echo 'TOOL_PID=$!' >> /custom-entrypoint.sh && \
    echo 'sleep 15' >> /custom-entrypoint.sh && \
    echo '# Use Python to fix config files - more reliable than sed' >> /custom-entrypoint.sh && \
    echo 'python3 << PYEOF' >> /custom-entrypoint.sh && \
    echo 'import os' >> /custom-entrypoint.sh && \
    echo 'import re' >> /custom-entrypoint.sh && \
    echo 'for root, dirs, files in os.walk("/etc/ldap/slapd.d"):' >> /custom-entrypoint.sh && \
    echo '    for file in files:' >> /custom-entrypoint.sh && \
    echo '        if file.endswith(".ldif"):' >> /custom-entrypoint.sh && \
    echo '            path = os.path.join(root, file)' >> /custom-entrypoint.sh && \
    echo '            with open(path, "r") as f:' >> /custom-entrypoint.sh && \
    echo '                content = f.read()' >> /custom-entrypoint.sh && \
    echo '            # Remove all olcListener lines with :tcp://' >> /custom-entrypoint.sh && \
    echo '            lines = content.split("\\n")' >> /custom-entrypoint.sh && \
    echo '            new_lines = []' >> /custom-entrypoint.sh && \
    echo '            for line in lines:' >> /custom-entrypoint.sh && \
    echo '                if "olcListener:" in line and ":tcp://" in line:' >> /custom-entrypoint.sh && \
    echo '                    continue' >> /custom-entrypoint.sh && \
    echo '                new_lines.append(line)' >> /custom-entrypoint.sh && \
    echo '            # Add correct listeners after dn: cn=config' >> /custom-entrypoint.sh && \
    echo '            if "cn=config.ldif" in path:' >> /custom-entrypoint.sh && \
    echo '                final_lines = []' >> /custom-entrypoint.sh && \
    echo '                for i, line in enumerate(new_lines):' >> /custom-entrypoint.sh && \
    echo '                    final_lines.append(line)' >> /custom-entrypoint.sh && \
    echo '                    if line == "dn: cn=config" and i+1 < len(new_lines) and not new_lines[i+1].startswith("olcListener:"):' >> /custom-entrypoint.sh && \
    echo '                        final_lines.append("olcListener: ldap://0.0.0.0:389")' >> /custom-entrypoint.sh && \
    echo '                        final_lines.append("olcListener: ldaps://0.0.0.0:636")' >> /custom-entrypoint.sh && \
    echo '                        final_lines.append("olcListener: ldapi:///")' >> /custom-entrypoint.sh && \
    echo '                new_lines = final_lines' >> /custom-entrypoint.sh && \
    echo '            with open(path, "w") as f:' >> /custom-entrypoint.sh && \
    echo '                f.write("\\n".join(new_lines))' >> /custom-entrypoint.sh && \
    echo 'PYEOF' >> /custom-entrypoint.sh && \
    echo 'kill $TOOL_PID 2>/dev/null || true' >> /custom-entrypoint.sh && \
    echo 'wait $TOOL_PID 2>/dev/null || true' >> /custom-entrypoint.sh && \
    echo '' >> /custom-entrypoint.sh && \
    echo '# Now start normally with patched config' >> /custom-entrypoint.sh && \
    echo 'exec /container/tool/run' >> /custom-entrypoint.sh && \
    chmod +x /custom-entrypoint.sh

# Replace /container/tool/run with a wrapper that fixes the issue
# The osixia entrypoint calls /container/tool/run, so we'll intercept it
RUN mv /container/tool/run /container/tool/run.orig && \
    cat > /container/tool/run << 'EOF'
#!/bin/bash
# Wrapper around osixia tool/run that fixes malformed URLs after startup

# Run the original tool/run but only for startup phase
/container/tool/run.orig --skip-start 2>&1 || {
  # If --skip-start doesn't work, run normally but intercept process.sh
  /container/tool/run.orig &
  TOOL_PID=$!
  sleep 20
  pkill -f "process/slapd/run" || true
  kill $TOOL_PID 2>/dev/null || true
  wait $TOOL_PID 2>/dev/null || true
}

# Fix malformed URLs in config
if [ -d /etc/ldap/slapd.d ]; then
  find /etc/ldap/slapd.d -name "*.ldif" -type f -exec sed -i '/olcListener:/d' {} \; 2>/dev/null || true
  find /etc/ldap/slapd.d -name "*.ldif" -type f -exec sed -i '/:tcp:\/\//d' {} \; 2>/dev/null || true
fi

# Start slapd directly
exec /usr/sbin/slapd -h "ldap://0.0.0.0:389 ldaps://0.0.0.0:636 ldapi:///" -F /etc/ldap/slapd.d -u openldap -g openldap -d 256
EOF
RUN chmod +x /container/tool/run
