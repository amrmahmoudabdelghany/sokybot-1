#!/bin/bash
# Verification script to check if dev-tools bundle is properly set up

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Verifying sokybot-dev-tools bundle setup..."
echo ""

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found. Please install Maven."
    exit 1
fi
echo "✅ Maven found"

# Check if pom.xml exists
if [ ! -f "pom.xml" ]; then
    echo "❌ pom.xml not found"
    exit 1
fi
echo "✅ pom.xml found"

# Check Java files
if [ ! -d "src/main/java/org/sokybot/devtools" ]; then
    echo "❌ Java source directory not found"
    exit 1
fi
echo "✅ Java source directory found"

# Check frontend directory
if [ ! -d "src/main/frontend" ]; then
    echo "❌ Frontend directory not found"
    exit 1
fi
echo "✅ Frontend directory found"

# Check frontend package.json
if [ ! -f "src/main/frontend/package.json" ]; then
    echo "❌ Frontend package.json not found"
    exit 1
fi
echo "✅ Frontend package.json found"

# Check if bundle compiles (quick compile check without building frontend)
echo ""
echo "Running quick compile check (frontend must be built separately)..."
if mvn compile -q 2>&1 | grep -i "error\|failure\|BUILD FAILURE"; then
    echo "❌ Compilation errors found. Check output above."
    echo "💡 Tip: Run './dev-frontend-build.sh' first if frontend build is needed"
    exit 1
fi
echo "✅ Compilation successful"

# Check ports availability (optional)
echo ""
echo "Checking ports..."
if command -v netstat &> /dev/null || command -v ss &> /dev/null; then
    if netstat -tuln 2>/dev/null | grep -q ":7001 " || ss -tuln 2>/dev/null | grep -q ":7001 "; then
        echo "⚠️  Port 7001 (HTTP) is already in use"
    else
        echo "✅ Port 7001 (HTTP) is available"
    fi
    
    if netstat -tuln 2>/dev/null | grep -q ":7002 " || ss -tuln 2>/dev/null | grep -q ":7002 "; then
        echo "⚠️  Port 7002 (RSocket) is already in use"
    else
        echo "✅ Port 7002 (RSocket) is available"
    fi
else
    echo "⚠️  Cannot check ports (netstat/ss not available)"
fi

echo ""
echo "=========================================="
echo "✅ Bundle verification complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Build the bundle: ./dev-build.sh"
echo "2. Install the bundle in your OSGi framework"
echo "3. Access the UI at: http://localhost:7001/devtools"
echo ""
