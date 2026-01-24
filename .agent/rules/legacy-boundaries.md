---
trigger: always_on
glob:
description: Guidelines for handling pre-existing issues and legacy code boundaries
---

# Pre-Existing Issues & Legacy Boundaries

## 1. The Causality Principle

Before attempting to fix ANY compilation error or test failure, perform a **Causality Check**:

| Question | If YES | If NO |
|----------|--------|-------|
| Did I modify this file? | You may fix it | **Pre-existing issue** |
| Did I modify a dependency this file uses? | You may fix imports/calls | **Pre-existing issue** |
| Is the error from a class/method I moved/deleted? | You may update references | **Pre-existing issue** |

**Pre-existing issues**: DO NOT ATTEMPT TO FIX. Report to user and continue.

## 2. Module Classification

### Legacy Modules (Read-Only Logic)
These modules have known technical debt. **Do NOT modify internal logic**:

| Module | Status | Allowed Changes |
|--------|--------|-----------------|
| `sokybot-engine` | Legacy/Complex | Imports only |
| `sokybot-game-events` | Generated/Large | None unless regenerating |
| `sokybot-persistence` | Complex Hibernate | Schema-related only |

### Stable Modules (Careful Changes)
These are stable but complex:
- `sokybot-proxy` - Network layer, test thoroughly
- `sokybot-security` - Cryptography, minimal changes
- `sokybot-pk2` / `sokybot-pk2-extractor` - File format handling

### Active Development (Normal Rules)
These modules can be freely modified:
- `sokybot-commons` - Utilities
- `sokybot-http-server` - HTTP/WebSocket API
- `sokybot-webview` - Web UI
- `sokybot-dev-tools` - Development utilities
- `sokybot-swing` - Swing UI components
- `sokybot-dist` - Distribution/packaging

## 3. Build Failure Protocol

When builds fail:

1. **Check if failure is in your Active Scope**
   - YES → Fix the issue
   - NO → Continue to step 2

2. **Check failure type**:
   | Failure Type | Action |
   |--------------|--------|
   | Symbol not found (your API change) | Fix the reference |
   | Pre-existing checkstyle | Skip, use `-Dcheckstyle.skip=true` |
   | Pre-existing test failure | Skip, use `-DskipTests` |
   | Unrelated compile error | Report to user, skip module |

3. **Resume strategy**: Build specific modules with `-pl module-name -am`

## 4. When to STOP and Ask

STOP immediately and ask the user when you encounter:
- Logic errors in modules you didn't touch
- Circular dependency issues
- OSGi resolution failures in legacy bundles
- Database/persistence schema issues
- Security-related code changes needed