#!/usr/bin/env bash
# Fast inner loop for sokybot-webview on Apache Karaf (developer reference).
# SNAPSHOT JARs are cached under $KARAF_HOME/data/cache — a plain bundle:update can
# keep serving an old copy. Prefer bundle:watch against your local Maven repo,
# or stop Karaf, clear data/cache for that bundle, or use update --force per your Karaf version.
#
# Usage (Karaf shell): source paths are illustrative — adjust groupId/version.
#   bundle:update --bundle <id>
# Or install from file after mvn install:
#   bundle:install -s file:/path/to/sokybot-webview/target/sokybot-webview-*.jar
#
set -euo pipefail
echo "See comments in $0 and ui/sokybot-webview/RSOCKET_SECURITY.md (Karaf / OSGi section)."
echo "After mvn -pl ui/sokybot-webview -am install, refresh the bundle in Karaf; if changes do not appear, clear data/cache."
echo "Docker dev: from the host run ./infra/scripts/soky backend watch-host (uses file:///app/... bundle:update)."
