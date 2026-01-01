# Git Hooks

This directory contains shared git hooks for the project.

## Installation

To install the git hooks, run:

```bash
.githooks/install-hooks.sh
```

This will copy all hooks from `.githooks/` to `.git/hooks/` and make them executable.

## Alternative: Configure Git to Use This Directory

You can configure git to use this directory directly (Git 2.9+):

```bash
git config core.hooksPath .githooks
```

This way, hooks are automatically used without needing to run the install script.

## Available Hooks

### pre-commit

Runs code formatting checks before every commit:

- Executes `make lint` to check code formatting
- If formatting issues are detected, automatically runs `make format`
- Prevents commits with formatting violations
- Can be bypassed with `git commit --no-verify` (not recommended)

**What it does:**
1. Checks code formatting using Spotless
2. If issues found, automatically fixes them with `make format`
3. Prompts you to review and stage the formatted changes
4. Blocks the commit until formatting is correct

**Benefits:**
- Ensures consistent code style across the team
- Catches formatting issues before they reach the repository
- Saves time in code reviews by enforcing style automatically

## Updating Hooks

If you modify a hook in `.githooks/`, other developers need to reinstall:

```bash
.githooks/install-hooks.sh
```

Or if using `core.hooksPath`, changes are picked up automatically.

## Bypassing Hooks

In rare cases where you need to bypass hooks (not recommended):

```bash
git commit --no-verify
```

Only use this when absolutely necessary (e.g., emergency hotfix).
