#!/bin/bash
# =============================================================================
# Sokybot Karaf Development Launcher
# =============================================================================
# This script starts the Karaf distribution in development mode with shell access.
# 
# Usage:
#   ./run-karaf.sh          - Start in interactive mode (with shell)
#   ./run-karaf.sh server   - Start in server mode (no shell, background)
#   ./run-karaf.sh debug    - Start with remote debugging on port 5005
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KARAF_HOME="$SCRIPT_DIR/../../infra/sokybot-dist/target/assembly"

# Check if distribution has been built
if [ ! -d "$KARAF_HOME" ]; then
    echo "========================================"
    echo " Karaf distribution not found!"
    echo "========================================"
    echo ""
    echo " Please build the distribution first:"
    echo ""
    echo "   mvn clean install -pl sokybot-features,sokybot-dist -am -P dev"
    echo ""
    echo " Or build the entire project:"
    echo ""
    echo "   mvn clean install"
    echo ""
    exit 1
fi

# Handle arguments
case "$1" in
    server)
        echo "Starting Sokybot in server mode (background)..."
        "$KARAF_HOME/bin/start"
        echo "Sokybot started. Use '$KARAF_HOME/bin/stop' to stop or './infra/scripts/soky backend logs' to view logs."
        ;;
    debug)
        echo "Starting Sokybot with remote debugging on port 5005..."
        export KARAF_DEBUG=true
        "$KARAF_HOME/bin/karaf"
        ;;
    *)
        echo "Starting Sokybot in interactive mode..."
        # Check if already running and offer to connect instead
        if pgrep -f "karaf.jar" > /dev/null; then
             echo "================================================================"
             echo " WARN: Sokybot appears to be running already."
             echo " connecting to existing instance..."
             echo "================================================================"
             "$KARAF_HOME/bin/client"
             exit 0
        fi

        # Enable hot reload (bundle:watch) in shell init script
        INIT_SCRIPT="$KARAF_HOME/etc/shell.init.script"
        if [ -f "$INIT_SCRIPT" ]; then
            if ! grep -q "bundle:watch" "$INIT_SCRIPT"; then
                echo "bundle:watch *" >> "$INIT_SCRIPT"
            fi
        fi
        "$KARAF_HOME/bin/karaf"
        ;;
esac
