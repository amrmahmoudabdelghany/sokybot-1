---
trigger: always_on
glob: "**/pom.xml"
description: Workspace directory structure and module organization
---

# Workspace Structure Rules

## 1. Directory Layout
Modules MUST be grouped into the following categories:

| Directory | Purpose | Examples |
|-----------|---------|----------|
| `core/` | Core logic and runtime | `sokybot-engine`, `sokybot-runtime` |
| `network/` | Network communication | `sokybot-proxy`, `sokybot-security` |
| `game/` | Game data and logic | `sokybot-game-model`, `sokybot-pk2` |
| `ui/` | User Interface | `sokybot-webview`, `sokybot-dev-tools`, `sokybot-frontend-shared` |
| `actuators/` | Bot logic extensions | `sokybot-actuator-login` |
| `infra/` | Shared infrastructure | `sokybot-commons`, `sokybot-persistence` |

## 2. Creating New Modules
When creating a new module:
1.  **Categorize**: Decide which directory it belongs to.
2.  **Location**: Create the folder INSIDE that category directory.
3.  **POM Relative Path**: Use `<relativePath>../../pom.xml</relativePath>`.
4.  **Registration**: Register the module in the **Category POM** (e.g., `core/pom.xml`), NOT the root POM.

## 3. Naming Convention
- Module directory names match artifactId (e.g., `sokybot-engine`).
- Category directory names are lowercase single words (`core`, `game`).
