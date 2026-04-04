#!/usr/bin/env bash
# Post-deploy smoke: call workspace.summary over RSocket/WebSocket (requires running backend).
#
# Usage:
#   ./scripts/rsocket-post-deploy-smoke.sh ws://127.0.0.1:8182/rsocket
#
# Runs an opt-in JUnit test in sokybot-webview (no Pax Exam / Karaf required).
set -euo pipefail
URL="${1:?Usage: $0 ws://HOST:PORT/rsocket}"
ROOT=$(cd "$(dirname "$0")/.." && pwd)
# clean avoids stale test bytecode confusing the bundle manifest on incremental builds
exec mvn -f "$ROOT/pom.xml" -pl ui/sokybot-webview clean test \
  -Dtest=RSocketWorkspaceSummarySmokeIT \
  -Dsokybot.rsocket.smoke.url="$URL"
