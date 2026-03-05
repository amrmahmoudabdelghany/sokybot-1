## Declarative Pages Inventory

This document tracks where Sokybot currently uses the declarative UI model, and where pages remain hybrid or imperative-only. It is intentionally high-level and will be refined over time.

Classification:

- **Pure declarative** – Layout and presentation fully in JSON schema; backend is a thin state provider (schema/state/actions/streams), React uses `DeclarativeExtensionView`.
- **Hybrid** – Uses JSON schema + `DeclarativeExtensionView`, but still contains significant logic in Groovy/React beyond state wiring.
- **Imperative** – Custom React/Electron UI without a JSON schema yet.

---

### Machine-scoped pages (`scripts/pages/*.groovy` + `scripts/pages/*.json`)

Source:

- Schemas: `scripts/pages/*.json`
- Pages: `scripts/pages/*.groovy`
- Loader/registrar: `ui/sokybot-machine-pages` (`MachinePagesActivator`, `ScriptPageLoader`)

#### Log

- Files:
  - Schema: `scripts/pages/Log.json`
  - Backend: `scripts/pages/Log.groovy`
  - Machine registrar: `ui/sokybot-machine-pages/src/main/java/org/sokybot/machinepages/MachinePagesActivator.java`
  - Group/system registrars: `ui/sokybot-webview/src/main/java/org/sokybot/webview/SystemLogPageRegistrar.java`, `GroupLogPageRegistrar.java`
- Classification: **Pure declarative (new model)**
- Notes:
  - Uses shared `Log.json` schema for machine, group, and system scopes.
  - Backed by `ILogStreamService` for snapshot + streaming.
  - Level/feature filters wired via actions and dynamic `streamParams`.

#### Training

- Files:
  - Schema: `scripts/pages/Training.json`
  - Backend: `scripts/pages/Training.groovy`
- Classification: **Hybrid (declarative + Groovy logic)**
- Notes:
  - Layout and bindings come from `Training.json`.
  - `Training.groovy` uses `ISettingsRegistry` to read/write training settings and emits state.
  - Further work: ensure all presentational/conditional logic is moved into the JSON schema; keep Groovy focused on settings/state only.

#### Healing

- Files:
  - Schema: `scripts/pages/Healing.json`
  - Backend: `scripts/pages/Healing.groovy`
- Classification: **Hybrid**
- Notes:
  - Similar pattern to Training: declarative schema plus Groovy settings/state logic.

#### Navigation

- Files:
  - Schema: `scripts/pages/Navigation.json`
  - Backend: `scripts/pages/Navigation.groovy`
- Classification: **Hybrid**
- Notes:
  - Handles navigation-related settings and state; presentation is in JSON.

#### Inventory

- Files:
  - Schema: `scripts/pages/Inventory.json`
  - Backend: `scripts/pages/Inventory.groovy`
- Classification: **Hybrid**
- Notes:
  - Exposes inventory data and controls; schema handles table/layout, Groovy handles data retrieval and updates.

#### Skills

- Files:
  - Schema: `scripts/pages/Skills.json`
  - Backend: `scripts/pages/Skills.groovy`
- Classification: **Hybrid**
- Notes:
  - Similar pattern to Training/Healing for skill configuration and display.

#### Environment

- Files:
  - Schema: `scripts/pages/Environment.json`
  - Backend: `scripts/pages/Environment.groovy`
- Classification: **Hybrid**

#### Connection

- Files:
  - Schema: `scripts/pages/Connection.json`
  - Backend: `scripts/pages/Connection.groovy`
- Classification: **Hybrid**

#### Packet Sniffer

- Files:
  - Schema: `scripts/pages/PacketSniffer.json` (with refs to `scripts/pages/traffic-monitor.json`, `scripts/pages/packet-tracer.json`)
  - Backend: `scripts/pages/PacketSniffer.groovy`
  - Bundle: `network/sokybot-packet-sniffer` provides `IPacketSnifferRegistry` and per-machine `PacketSnifferService`; scripted page delegates to it.
- Classification: **Pure declarative (scripted page + bundle API)**
- Notes:
  - State, actions, and streams (packets, statistics) are provided by the packet-sniffer bundle via `IPacketSnifferPage`; Groovy page delegates. MachinePagesActivator wires stream handlers and state-change listener.

#### Packet Analyzer

- Files:
  - Schema: `scripts/pages/PacketAnalyzer.json`
  - Backend: `scripts/pages/PacketAnalyzer.groovy`
- Classification: **Pure declarative (scripted page + bundle API)**
- Notes:
  - Analyzer state and actions (selectHex, defineVariable, etc.) are delegated to the same packet-sniffer service via `getAnalyzerState()` and `handleAction("analyzerAction", ...)`.

Summary:

- All **machine tools** (Training, Healing, Navigation, Inventory, Skills, Environment, Connection, Log, Packet Sniffer, Packet Analyzer) use JSON schemas and scripted pages.
- The **Log** page is fully aligned with the new declarative + streaming model.
- Other machine pages are **hybrid**: structurally declarative, with some view-like behaviour in Groovy that can be gradually moved into schemas.

---

### Global/group pages (registered via `IWebviewConfigurator`)

#### System Log

- Files:
  - Schema: `scripts/pages/Log.json`
  - Registrar: `ui/sokybot-webview/src/main/java/org/sokybot/webview/SystemLogPageRegistrar.java`
- Classification: **Pure declarative (new model)**
- Notes:
  - Uses `ILogStreamService.getRecentEntriesForSystem` and `getStreamForSystem`.
  - Uses `Log.level` for filtering; `Log.feature` reserved for future use.

#### Group Log

- Files:
  - Schema: `scripts/pages/Log.json`
  - Registrar: `ui/sokybot-webview/src/main/java/org/sokybot/webview/GroupLogPageRegistrar.java`
- Classification: **Pure declarative (new model)**
- Notes:
  - Per-group pages with `"GroupLog_<groupName>"` page IDs.
  - Uses `ILogStreamService.getRecentEntriesForGroup` and `getStreamForGroup`.

---

### Network tools and packet inspection

Packet Sniffer and Packet Analyzer are now **machine-scoped scripted pages** (see above under Machine-scoped pages). The `network/sokybot-packet-sniffer` bundle no longer registers webview pages; it exposes `IPacketSnifferRegistry` and per-machine `IPacketSnifferPage` for the scripted pages to use.

#### Dev tools UI (removed / in main webview)

- Files:
  - React frontend: `ui/sokybot-dev-tools/src/main/frontend/src/*.tsx`
- Classification: **Imperative**
- Notes:
  - Dev tools app is currently implemented as a custom React SPA.
  - It talks to the backend via its own RSocket client.
  - Candidate for gradual migration:
    - Expose core views (metrics, database, services, bundles, logs) as declarative pages over time.

---

### Webview main application

- Files:
  - `ui/sokybot-webview/src/main/frontend/src/App.tsx`
  - `ui/sokybot-webview/src/main/frontend/src/Layout.tsx`
  - `ui/sokybot-webview/src/main/frontend/src/MachineView.tsx`
  - `ui/sokybot-webview/src/main/frontend/src/extensions/DeclarativeExtensionView.tsx`
  - `ui/sokybot-webview/src/main/frontend/src/extensions/ExtensionView.tsx`
- Classification:
  - **App / Layout / MachineView** – **Imperative scaffolding** (shell that hosts declarative pages).
  - `DeclarativeExtensionView` / `ExtensionView` – **Infrastructure** for declarative rendering.
- Notes:
  - The shell (sidebar, routing) is intentionally imperative.
  - All feature-specific content should be declarative pages rendered through `ExtensionView` / `DeclarativeExtensionView`.

---

### Settings & configuration UIs

- Settings and configuration UIs are mostly surfaced through the machine pages:
  - Training, Healing, Navigation, Inventory, Skills, Environment, Connection.
- There is currently **no single consolidated declarative settings dashboard**, but:
  - All these pages already interact with `ISettingsRegistry` via their Groovy pages.
  - They are good candidates for:
    - Standardized form components in schemas.
    - Consistent `update`/`save`/`reset` patterns.

Classification summary:

- **Pure declarative (new model)**
  - Machine Log pages.
  - Group Log pages.
  - System Log page.
  - Packet Sniffer and Packet Analyzer (scripted pages under `scripts/pages/`, backed by `network/sokybot-packet-sniffer` API).
- **Hybrid**
  - Machine Training, Healing, Navigation, Inventory, Skills, Environment, Connection pages.
- **Imperative**
  - Webview shell (App, Layout, MachineView).

This inventory is the baseline for future migrations:

- Machine tools: converge toward the pure declarative pattern used by logs and packet monitor.
- Dev tools: introduce declarative pages for core views over time.
- Settings: standardize declarative forms across all configuration-heavy areas.

