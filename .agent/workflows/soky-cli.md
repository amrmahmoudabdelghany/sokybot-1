---
description: How to use the soky CLI for development tasks
---

# Soky CLI Usage

The `soky` CLI is a unified tool for managing the Sokybot development environment.

## Installation

```bash
# From the project root
./infra/scripts/soky install
```

This creates a symlink in `~/bin/soky`. Ensure `~/bin` is in your PATH.

## Common Workflows

### Starting Development

```bash
# Start the backend (Karaf)
soky backend start

# Start frontend dev servers (Webview + DevTools)
soky ui dev

# Check status of all services
soky status
```

### Making Code Changes

After modifying a bundle's source code:

```bash
# Option 1: Build and deploy with cache clear (recommended)
soky deploy sokybot-webview

# Option 2: Manual steps
soky build module sokybot-webview
soky backend restart --fresh
```

**Important**: Always use `--fresh` or `deploy` after code changes to avoid stale bundle caching issues.

### Building

```bash
# Build entire backend
soky build backend

# Build with fast profile (skip tests/checks)
soky build backend --fast

# Build specific module (auto-finds nested modules)
soky build module sokybot-webview

# Build everything
soky build all
```

### Backend Management

```bash
soky backend start        # Start in background
soky backend stop         # Stop gracefully
soky backend restart      # Restart (may use cached bundles)
soky backend restart --fresh  # Restart with cache clear
soky backend debug        # Start with debugger on port 5005
soky backend logs         # Tail Karaf logs
soky backend shell        # Connect to Karaf shell
soky backend clean        # Clear Karaf data directory
```

### Troubleshooting

```bash
# Force kill all processes
soky kill

# Check environment setup
soky setup

# View detailed status
soky status
```

## Deploy Command Details

The `deploy` command is the recommended way to apply code changes:

```bash
soky deploy <module-name> [--no-restart]
```

**What it does:**
1. Finds the module (searches `ui/`, `core/`, `network/`, `infra/`)
2. Builds and installs to Maven repo
3. Stops the backend
4. Clears Karaf cache (removes stale bundles)
5. Starts the backend

**Options:**
- `--no-restart`: Try hot reload via Karaf (may not work for all changes)

## Common Issues

| Issue | Solution |
|-------|----------|
| "Method not found" RSocket errors | Use `soky deploy` or `soky backend restart --fresh` |
| Module not found in reactor | Use `soky build module <name>` (auto-finds nested modules) |
| Backend won't stop | Use `soky kill` to force terminate |
| Old code still running | Clear cache: `soky backend clean && soky backend start` |
