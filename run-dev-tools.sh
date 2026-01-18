#!/bin/bash
# Run script for starting Sokybot with Dev Tools

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "Starting Sokybot Application"
echo "=========================================="
echo ""
echo "Dev Tools will be accessible at:"
echo "  - HTTP: http://localhost:7001/devtools"
echo "  - RSocket: ws://localhost:7002"
echo ""
echo "Press Ctrl+C to stop"
echo ""

# Check if bootstrap is built
if [ ! -f "sokybot-bootstrap/target/sokybot-bootstrap-1.0-SNAPSHOT.jar" ]; then
    echo "Building bootstrap module..."
    cd sokybot-bootstrap
    mvn package -DskipTests -q
    cd ..
fi

# Check if dev-tools bundle exists in plugins
if [ ! -f "plugins/sokybot-dev-tools-1.0-SNAPSHOT.jar" ]; then
    echo "Building dev-tools bundle..."
    cd sokybot-dev-tools
    ./dev-build.sh
    cd ..
fi

# Run the application
echo "Starting application..."
java -cp "sokybot-bootstrap/target/sokybot-bootstrap-1.0-SNAPSHOT.jar:sokybot-bootstrap/target/dependency/*" \
     org.sokybot.SokybotLauncher
