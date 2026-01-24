#!/bin/bash
# =============================================================================
# Sokybot Development Launcher (Legacy Wrapper)
# =============================================================================
# This script is a wrapper around run-karaf.sh for backward compatibility.
# It ensures the 'dev' profile is used.
# =============================================================================

echo "=================================================="
echo " Note: Sokybot now uses Karaf for runtime."
echo " This script is a wrapper for: ./run-karaf.sh"
echo "=================================================="
echo ""

# Ensure we are in the correct directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Execute run-karaf.sh
./run-karaf.sh "$@"
