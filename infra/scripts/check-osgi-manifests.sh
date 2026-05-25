#!/usr/bin/env bash
# OSGi manifest / bnd hygiene checks for sokybot.
# - Built JAR duplicate Import-Package / Export-Package entries
# - bnd Import-Package overlap (explicit sub-package after covering wildcard)
# - *-api bundle bnd.bnd hygiene (existence, exports, versions, source coverage)
set -euo pipefail

ROOT="${1:-$(cd "$(dirname "$0")/../.." && pwd)}"
cd "$ROOT"

FAIL=0
API_VIOLATIONS=0
API_MODULES=0

# --- Manifest duplicate check on built JARs ---
check_manifest_duplicates() {
  local jar="$1"
  local tmp
  tmp=$(mktemp)
  if ! unzip -p "$jar" META-INF/MANIFEST.MF >"$tmp" 2>/dev/null; then
    rm -f "$tmp"
    return 0
  fi

  for header in Import-Package Export-Package; do
    python3 - "$tmp" "$header" "$jar" <<'PY' || return 1
import re, sys
path, header, jar = sys.argv[1], sys.argv[2], sys.argv[3]
text = open(path, encoding="utf-8", errors="replace").read()
lines = []
for line in text.splitlines():
    if line.startswith(" ") and lines:
        lines[-1] += line[1:]
    else:
        lines.append(line)
value = None
for line in lines:
    if line.startswith(header + ":"):
        value = line.split(":", 1)[1].strip()
        break
if not value:
    sys.exit(0)

def split_packages(val):
    parts, cur, depth, in_quote = [], [], 0, False
    for ch in val:
        if ch == '"':
            in_quote = not in_quote
            cur.append(ch)
        elif not in_quote and ch == "[":
            depth += 1
            cur.append(ch)
        elif not in_quote and ch == "]":
            depth = max(0, depth - 1)
            cur.append(ch)
        elif not in_quote and ch == "," and depth == 0:
            parts.append("".join(cur).strip())
            cur = []
        else:
            cur.append(ch)
    if cur:
        parts.append("".join(cur).strip())
    return [p for p in parts if p]

parts = split_packages(value)
names = []
for p in parts:
    name = p.split(";")[0].strip().strip('"')
    if name.startswith("!"):
        continue
    names.append(name)
seen = {}
dups = []
for n in names:
    if n in seen:
        dups.append(n)
    seen[n] = seen.get(n, 0) + 1
if dups:
    print(f"ERROR: duplicate {header} in {jar}: {sorted(set(dups))}", file=sys.stderr)
    sys.exit(1)
PY
    if [ $? -ne 0 ]; then
      FAIL=1
    fi
  done
  rm -f "$tmp"
}

echo "=== OSGi manifest duplicate check ==="
JAR_COUNT=0
while IFS= read -r -d '' jar; do
  case "$jar" in
    *-sources.jar|*-javadoc.jar|*/original-*|*/assembly/*|*/target/dependency/*) continue ;;
  esac
  case "$(basename "$jar")" in
    sokybot-*.jar) ;;
    *) continue ;;
  esac
  JAR_COUNT=$((JAR_COUNT + 1))
  if ! check_manifest_duplicates "$jar"; then
    FAIL=1
  fi
done < <(find . -path '*/target/*.jar' -print0 2>/dev/null)

if [ "$JAR_COUNT" -eq 0 ]; then
  echo "WARN: no target/*.jar found — run mvn package/install first"
fi

# --- bnd.bnd Import-Package overlap (explicit after wildcard covers it) ---
echo "=== bnd Import-Package overlap check ==="
set +e
python3 <<'PY'
import re, sys
from pathlib import Path

root = Path(".")
fail = False

def parse_import_clauses(text):
    m = re.search(r"^Import-Package:\s*\\?\s*$", text, re.M)
    if not m:
        m2 = re.search(r"^Import-Package:\s*(.+)$", text, re.M)
        if not m2:
            return []
        block = m2.group(1)
    else:
        start = m.end()
        block_lines = []
        for line in text[start:].splitlines():
            if line and not line[0].isspace() and not line.endswith("\\") and block_lines and not block_lines[-1].rstrip().endswith("\\"):
                if re.match(r"^[A-Za-z0-9_-]+:", line):
                    break
            if re.match(r"^[A-Za-z0-9_-]+:", line) and block_lines:
                break
            block_lines.append(line)
        block = " ".join(l.strip().rstrip("\\").strip() for l in block_lines)
    clauses = [c.strip() for c in block.split(",") if c.strip()]
    parsed = []
    for c in clauses:
        if c.startswith("!"):
            continue
        pkg = c.split(";")[0].strip()
        if pkg.startswith("-include:"):
            continue
        parsed.append(pkg)
    return parsed

def is_subpackage(child, parent_wildcard):
    if not parent_wildcard.endswith(".*"):
        return False
    prefix = parent_wildcard[:-2]
    if child == prefix:
        return True
    return child.startswith(prefix + ".")

for bnd in sorted(root.glob("**/bnd.bnd")):
    if "target" in bnd.parts:
        continue
    text = bnd.read_text(encoding="utf-8", errors="replace")
    clauses = parse_import_clauses(text)
    if not clauses:
        continue
    for i, explicit in enumerate(clauses):
        if explicit.endswith(".*"):
            continue
        for j, other in enumerate(clauses):
            if j < i and other.endswith(".*") and is_subpackage(explicit, other):
                print(f"ERROR: {bnd}: explicit '{explicit}' at index {i} follows wildcard '{other}' at {j}", file=sys.stderr)
                fail = True
                break

if fail:
    sys.exit(1)
PY
BND_EXIT=$?
set -e
if [ "$BND_EXIT" -ne 0 ]; then
  FAIL=1
fi

# --- *-api bundle bnd.bnd hygiene ---
echo "=== *-api bnd.bnd hygiene check ==="
set +e
API_CHECK_OUTPUT=$(python3 <<'PY' 2>&1
import re
import sys
from pathlib import Path

root = Path(".")
api_roots = ("core", "game", "infra", "ui", "actuators")
violations = []
api_modules = []

def is_api_module(path: Path) -> bool:
    name = path.name
    return name.endswith("-api") and (path / "pom.xml").is_file()

def discover_api_modules():
    found = []
    for top in api_roots:
        base = root / top
        if not base.is_dir():
            continue
        for child in sorted(base.iterdir()):
            if child.is_dir() and is_api_module(child):
                found.append(child)
    return found

def parse_package_list(text: str, header: str):
    m = re.search(rf"^{re.escape(header)}:\s*\\?\s*$", text, re.M)
    if not m:
        m2 = re.search(rf"^{re.escape(header)}:\s*(.+)$", text, re.M)
        if not m2:
            return []
        block = m2.group(1)
    else:
        start = m.end()
        block_lines = []
        for line in text[start:].splitlines():
            if re.match(r"^[A-Za-z0-9_-]+:", line) and block_lines:
                if not block_lines[-1].rstrip().endswith("\\"):
                    break
            block_lines.append(line)
        block = " ".join(l.strip().rstrip("\\").strip() for l in block_lines)
    entries = []
    for part in block.split(","):
        part = part.strip()
        if part:
            entries.append(part)
    return entries

def pkg_name(entry: str) -> str:
    return entry.split(";")[0].strip()

def package_covered(pkg: str, entries) -> bool:
    for entry in entries:
        base = pkg_name(entry)
        if base == pkg:
            return True
        if base.endswith(".*"):
            prefix = base[:-2]
            if pkg == prefix or pkg.startswith(prefix + "."):
                return True
    return False

def source_packages(module: Path):
    java_root = module / "src" / "main" / "java"
    if not java_root.is_dir():
        return set()
    pkgs = set()
    for java_file in java_root.rglob("*.java"):
        rel = java_file.relative_to(java_root)
        if len(rel.parts) < 2:
            continue
        pkgs.add(".".join(rel.parts[:-1]))
    return pkgs

for module in discover_api_modules():
    rel = module.relative_to(root)
    api_modules.append(str(rel))
    bnd_path = module / "bnd.bnd"
    if not bnd_path.is_file():
        violations.append(
            f"ERROR: {rel}: missing bnd.bnd — see .agent/skills/osgi-bundle-creation/SKILL.md"
        )
        continue

    text = bnd_path.read_text(encoding="utf-8", errors="replace")

    if not re.search(r"^Bundle-SymbolicName:\s*\S", text, re.M):
        violations.append(f"ERROR: {rel}: missing Bundle-SymbolicName")

    export_entries = parse_package_list(text, "Export-Package")
    private_entries = parse_package_list(text, "Private-Package")
    covered = export_entries + private_entries

    for entry in export_entries:
        if ";version=" not in entry:
            violations.append(
                f'ERROR: {rel}: Export-Package entry "{entry}" has no ;version= attribute — '
                "bnd will default to 0.0.0 which breaks versioned consumers"
            )

    for pkg in sorted(source_packages(module)):
        if not package_covered(pkg, covered):
            violations.append(
                f"ERROR: {rel}: source package {pkg} is neither exported nor marked Private-Package"
            )

# Non-*-api modules: warn only when bnd.bnd is missing but module has Java sources
for bnd in sorted(root.glob("**/bnd.bnd")):
    if "target" in bnd.parts:
        continue
    module = bnd.parent
    if is_api_module(module):
        continue
    # only warn for bundle-like modules with pom.xml
    if not (module / "pom.xml").is_file():
        continue

warn_missing = []
# scan bundle modules without bnd.bnd (warning only)
for top in ("core", "game", "infra", "ui", "network", "actuators"):
    base = root / top
    if not base.is_dir():
        continue
    for child in sorted(base.iterdir()):
        if not child.is_dir() or not (child / "pom.xml").is_file():
            continue
        if child.name.endswith("-api"):
            continue
        pom = (child / "pom.xml").read_text(encoding="utf-8", errors="replace")
        if "<packaging>bundle</packaging>" not in pom:
            continue
        if not (child / "bnd.bnd").is_file():
            warn_missing.append(f"WARN: {child.relative_to(root)}: missing bnd.bnd (non-api, advisory only)")

for v in violations:
    print(v, file=sys.stderr)
for w in warn_missing:
    print(w)

print(f"__API_MODULES__:{len(api_modules)}", file=sys.stdout)
print(f"__API_VIOLATIONS__:{len(violations)}", file=sys.stdout)
sys.exit(1 if violations else 0)
PY
)
API_PY_EXIT=$?
set -e
API_VIOLATIONS=$(echo "$API_CHECK_OUTPUT" | grep '^__API_VIOLATIONS__:' | cut -d: -f2- || echo 0)
API_MODULES=$(echo "$API_CHECK_OUTPUT" | grep '^__API_MODULES__:' | cut -d: -f2- || echo 0)
echo "$API_CHECK_OUTPUT" | grep -v '^__API_' || true

if [ "$API_PY_EXIT" -ne 0 ]; then
  FAIL=1
fi

if [ "$FAIL" -ne 0 ]; then
  if [ "${API_VIOLATIONS:-0}" -gt 0 ]; then
    echo "OSGi manifest check: ${API_VIOLATIONS} violations across ${API_MODULES} modules" >&2
  fi
  echo "OSGi manifest / bnd checks FAILED" >&2
  exit 1
fi

if [ "${API_MODULES:-0}" -gt 0 ]; then
  echo "OSGi manifest check: all ${API_MODULES} *-api modules OK"
else
  echo "OSGi manifest check: no *-api modules discovered"
fi
echo "OSGi manifest and bnd checks OK ($JAR_COUNT JARs scanned)"
