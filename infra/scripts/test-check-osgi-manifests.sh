#!/usr/bin/env bash
# Self-test for infra/scripts/check-osgi-manifests.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CHECK="$ROOT/infra/scripts/check-osgi-manifests.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

fail() { echo "FAIL: $*" >&2; exit 1; }
pass() { echo "PASS: $*"; }

run_check() {
  bash "$CHECK" "$TMP" 2>&1
}

make_api_module() {
  local rel="$1"
  local bnd="$2"
  local pkg="$3"
  mkdir -p "$TMP/$rel/src/main/java/$pkg"
  touch "$TMP/$rel/src/main/java/$pkg/Placeholder.java"
  cat >"$TMP/$rel/pom.xml" <<EOF
<project>
  <artifactId>sokybot-test-api</artifactId>
  <packaging>bundle</packaging>
</project>
EOF
  if [ -n "$bnd" ]; then
    printf '%s\n' "$bnd" >"$TMP/$rel/bnd.bnd"
  fi
}

# --- violation: missing bnd.bnd ---
rm -rf "$TMP"/*
mkdir -p "$TMP/core"
make_api_module "core/sokybot-test-missing-bnd-api" "" "org/sokybot/test/missing"
out="$(run_check || true)"
echo "$out" | grep -q 'missing bnd.bnd' || fail "expected missing bnd.bnd error"
pass "missing bnd.bnd"

# --- violation: missing Bundle-SymbolicName ---
rm -rf "$TMP"/*
make_api_module "core/sokybot-test-no-symbolic-api" \
  $'Bundle-Name: test\nExport-Package: org.sokybot.test.no.symbolic;version="${project.version}"' \
  "org/sokybot/test/no/symbolic"
out="$(run_check || true)"
echo "$out" | grep -q 'missing Bundle-SymbolicName' || fail "expected missing Bundle-SymbolicName error"
pass "missing Bundle-SymbolicName"

# --- violation: unversioned export ---
rm -rf "$TMP"/*
make_api_module "core/sokybot-test-unversioned-api" \
  $'Bundle-SymbolicName: org.sokybot.test.unversioned\nExport-Package: org.sokybot.test.unversioned' \
  "org/sokybot/test/unversioned"
out="$(run_check || true)"
echo "$out" | grep -q 'has no ;version= attribute' || fail "expected unversioned export error"
pass "unversioned Export-Package"

# --- violation: uncovered source package ---
rm -rf "$TMP"/*
make_api_module "core/sokybot-test-uncovered-api" \
  $'Bundle-SymbolicName: org.sokybot.test.uncovered\nExport-Package: org.sokybot.test.other;version="${project.version}"' \
  "org/sokybot/test/uncovered"
out="$(run_check || true)"
echo "$out" | grep -q 'source package org.sokybot.test.uncovered is neither exported' || fail "expected uncovered source package error"
pass "uncovered source package"

# --- known-good module (uses real repo module path copied minimally) ---
rm -rf "$TMP"/*
mkdir -p "$TMP/core/sokybot-test-good-api/src/main/java/org/sokybot/test/good"
touch "$TMP/core/sokybot-test-good-api/src/main/java/org/sokybot/test/good/Good.java"
cat >"$TMP/core/sokybot-test-good-api/pom.xml" <<'EOF'
<project><artifactId>sokybot-test-good-api</artifactId><packaging>bundle</packaging></project>
EOF
cat >"$TMP/core/sokybot-test-good-api/bnd.bnd" <<'EOF'
Bundle-SymbolicName: org.sokybot.test.good
Export-Package: org.sokybot.test.good;version="${project.version}"
EOF
out="$(run_check)"
echo "$out" | grep -q 'all 1 \*-api modules OK' || fail "expected good module to pass api check"
pass "known-good module"

echo "All check-osgi-manifests self-tests passed"
