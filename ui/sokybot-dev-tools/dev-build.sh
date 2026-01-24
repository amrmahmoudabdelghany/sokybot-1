#!/bin/bash
# Build script for sokybot-dev-tools bundle

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Building sokybot-dev-tools bundle..."

# Clean and build
mvn clean install -DskipTests

echo "Build complete!"
echo "Bundle location: target/sokybot-dev-tools-*.jar"
