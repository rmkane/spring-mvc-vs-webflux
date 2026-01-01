#!/bin/bash

# Script to install git hooks from .githooks directory to .git/hooks

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HOOKS_DIR="$SCRIPT_DIR"
GIT_HOOKS_DIR="$SCRIPT_DIR/../.git/hooks"

echo "Installing git hooks..."

# Copy all executable files from .githooks to .git/hooks
for hook in "$HOOKS_DIR"/*; do
    # Skip this script itself
    if [ "$(basename "$hook")" = "install-hooks.sh" ] || [ "$(basename "$hook")" = "README.md" ]; then
        continue
    fi

    # Skip non-files
    if [ ! -f "$hook" ]; then
        continue
    fi

    hook_name=$(basename "$hook")
    target="$GIT_HOOKS_DIR/$hook_name"

    # Copy and make executable
    cp "$hook" "$target"
    chmod +x "$target"

    echo "✅ Installed: $hook_name"
done

echo ""
echo "Git hooks installed successfully!"
echo ""
echo "Installed hooks:"
ls -1 "$GIT_HOOKS_DIR" | grep -v ".sample" || echo "  (none)"
