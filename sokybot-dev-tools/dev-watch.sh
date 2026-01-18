#!/bin/bash
# Watch script for auto-building frontend during development
# This starts the Vite dev server which serves the app and watches for changes

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/src/main/frontend"

echo "=========================================="
echo "Starting DevTools Frontend Dev Server"
echo "=========================================="
echo ""
echo "The dev server will run on http://localhost:3000"
echo "For production, build the frontend with: ./dev-frontend-build.sh"
echo ""

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

# Start dev server
echo "Starting Vite dev server..."
npm run dev
