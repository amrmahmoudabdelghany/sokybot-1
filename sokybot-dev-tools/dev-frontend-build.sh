#!/bin/bash
# Build only the frontend

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/src/main/frontend"

echo "Building frontend..."

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

# Build
npm run build

echo "Frontend build complete!"
echo "Output: ../target/webapp"
