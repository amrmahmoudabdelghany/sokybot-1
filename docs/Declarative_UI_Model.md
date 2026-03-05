## Sokybot Declarative UI Model

### Overview

Sokybot's primary UI for operators (Electron/React webview) is moving to a **declarative-first** model. In this model:

- **Page layout and presentation** are described in JSON *schemas*.
- **State and behaviour** come from backend **state providers**, **actions**, and **streams**.
- The React layer (`DeclarativeExtensionView`) is a thin renderer for these schemas and state, not the place for feature logic.

This document summarizes the current pattern so new features can follow it consistently.

---

### Core building blocks

#### 1. Schemas (JSON)

Schemas live in:

- `scripts/pages/*.json` for machine-scoped pages (e.g. `Log.json`, `Training.json`, `Healing.json`, `Navigation.json`, `Inventory.json`, `Skills.json`, `Environment.json`, `Connection.json`, `PacketSniffer.json`, `PacketAnalyzer.json`, and refs such as `traffic-monitor.json`, `packet-tracer.json`).

A schema describes:

- **Layout containers** (`div`, `layout`, `Card`, `CardHeader`, `CardContent`, etc.).
- **Primitive components** (inputs, selects, buttons, tables, custom components like `HexViewer`).
- **Bindings** to the page state (`context`) using template expressions like:
  - `"${Log && Log.maxEvents}"`, `"${settings.autoAttack}"`, `"${item.timestamp}"`.
- **Actions** referenced by string names in props:
  - `onClick: "clearLog"`, `onChange: "setLogLevel"`, `onRowClick: "selectPacket"`.
- **Streams**:
  - Either via dedicated `type: "stream"` nodes.
  - Or via `props.streamId`, `props.stateKey`, and `props.streamParams` on any component.

Examples:

- `scripts/pages/Log.json` – machine/group/system log view, with:
  - `Log.events`, `Log.maxEvents`, `Log.level`, `Log.feature`.
  - Actions `setLogLevel`, `setLogFeature`, `clearLog`.
  - A `stream` configuration wiring `streamId = "Log"` to the backend logging stream.
- `scripts/pages/PacketSniffer.json` and `scripts/pages/traffic-monitor.json` – declarative packet monitor, with:
  - `streamId: "packets"`, `stateKey: "trafficPackets"`, `streamMode: "append"`.
  - A table that binds to `trafficPackets`. Backed by scripted page `PacketSniffer.groovy` and `network/sokybot-packet-sniffer` bundle API (`IPacketSnifferPage`, `IPacketSnifferRegistry`).

#### 2. State providers (backend)

Each page has one or more backend components responsible for:

- Returning an initial **schema** and **state**.
- Handling **actions**.
- Providing **stream** data when needed.

Main patterns:

- **Machine-scoped Groovy pages** (`scripts/pages/*.groovy`) implementing `IScriptedPage`:
  - Example: `Log.groovy`, `Training.groovy`, `Healing.groovy`, etc.
  - These are loaded and wired by `MachinePagesActivator` in `ui/sokybot-machine-pages`.
  - They typically:
    - Implement `getSchema()` (often empty, schema loaded from JSON by `ScriptPageLoader`).
    - Implement `getInitialState()` to populate the page state.
    - Implement `handleAction(...)` to process actions from the UI.
    - Optionally implement `streamData(...)` for legacy streaming (superseded by central logging for logs).
- **Central Java registrars** for global/group pages:
  - `SystemLogPageRegistrar` and `GroupLogPageRegistrar` in `ui/sokybot-webview`:
    - Load `scripts/pages/Log.json`.
    - Call `IWebviewConfigurator.addDeclarativePage(...)` to register a page.
    - Register **schema handlers**, **action handlers**, and **stream handlers**.

For logging, the state providers are backed by a shared service:

- `ILogStreamService` (`infra/sokybot-logging`) + `LogStreamServiceImpl`:
  - Listens to OSGi `LogReaderService`.
  - Normalizes entries into a stable JSON model (`id`, `timestamp`, `level`, `category`, `source`, `thread`, `message`, `stackTrace`, `metadata`).
  - Uses MDC (`sokybot.log.*`) to populate `metadata.machineFullName`, `metadata.groupName`, `metadata.feature`, and a strict `category` (`SYSTEM`, `GROUP`, `MACHINE`).
  - Exposes:
    - `getStream(...)`, `getStreamForGroup(...)`, `getStreamForSystem(...)` – live Flux streams with filtering.
    - `getRecentEntries*` – bounded snapshots for initial state.

#### 3. Actions

Actions are referenced from schemas as strings:

- `onClick: "clearLog"`, `onChange: "setLogLevel"`, `onBlur: "update"` etc.

On the frontend:

- `ComponentRenderer` resolves these into callbacks that call:
  - `onAction(actionName, data)` passed from `DeclarativeExtensionView`.
- `DeclarativeExtensionView` sends actions via RSocket:
  - `rsocketService.request('extension.action', { pageId, action, data })`.
- It expects a response object that may contain:
  - `delta` – a partial update to merge into existing state (preferred for event streams).
  - `state` – a full or partial state object to merge.

On the backend:

- Groovy `IScriptedPage.handleAction` implementations or Java action handlers registered via:
  - `IWebviewConfigurator.registerActionHandler(pageId, handler)`.
- Actions typically:
  - Validate input.
  - Update settings or invoke services.
  - Return `state` or `delta` that updates the page.

Logging example:

- `Log.groovy` defines `clearLog`, `setMaxEvents`, `setLogLevel`, `setLogFeature`.
- For machine pages where `ILogStreamService` is active, actions primarily update `Log.level` and `Log.feature` so the frontend can adjust `streamParams`.
- `SystemLogPageRegistrar` and `GroupLogPageRegistrar` register action handlers that update `Log.level` / `Log.feature` for their scopes.

#### 4. Streams

Streams provide real-time updates to declarative pages.

Frontend:

- `DeclarativeExtensionView` scans the schema (`ComponentRenderer` is purely visual):
  - Looks for `type: "stream"` or components with `props.streamId`.
  - Reads:
    - `streamId`: logical stream name (`"Log"`, `"packets"`, etc.).
    - `stateKey`: top-level state key to populate (or falls back to `streamId`).
    - `streamMode`: how to merge (`"append"` or replace).
    - `streamParams`: arbitrary key/value pairs (possibly with template expressions like `"${Log.level}"`).
- For each stream, it:
  - Resolves `streamParams` against the current state (`data`).
  - Subscribes via `rsocketService.subscribe('extension.stream:${pageId}:${streamId}', handler, errorHandler, { ...params, pageId })`.
  - Updates state either:
    - Append mode: pushes each new `streamData` into an array.
    - Replace mode: stores `streamData` at `state[stateKey]`.
- **Dynamic filtering**:
  - When `streamParams` contain template expressions (e.g. `${Log.level}`, `${Log.feature}`), `DeclarativeExtensionView`:
    - Tracks the last resolved values per `streamId`.
    - On any state change, recomputes `streamParams`.
    - If they changed, unsubscribes and re-subscribes with new parameters.

Backend:

- Streams are registered via `IWebviewConfigurator.registerStreamHandler(pageId, streamId, handler, stateKey)`.
- `handler` receives a parameter map (derived from `streamParams`) and returns a `Flux<Map<String,Object>>`:
  - Typically built as:
    - `Flux.concat(Flux.just(initialState), liveUpdates)`:
      - `initialState` is built from a snapshot.
      - `liveUpdates` uses `scan(...)` to accumulate events into a bounded structure (log events list, metric snapshots, etc.).
- Examples:
  - Machine log pages: `MachinePagesActivator` registers `"Log"` stream with:
    - `getRecentEntries(machineFullName, level, feature, maxEntries)`.
    - `getStream(machineFullName, level, feature, maxEntries)` and `scan` into `[events, maxEvents]`.
  - Group/System logs: `GroupLogPageRegistrar` / `SystemLogPageRegistrar` register `"Log"` streams against `ILogStreamService.getStreamForGroup/getStreamForSystem`.
  - Packet monitor: `traffic-monitor.json` uses a `packets` stream with `includeHistory` parameter.

---

### Conventions and best practices

1. **One schema per page**
   - Place shared page schemas under `scripts/pages/` when they are reused across scopes (e.g. `Log.json`).
   - For feature-local tools, keep schemas near the feature (e.g. `ui/feature-x/src/main/resources/ui/*.json` or an equivalent resources location).

2. **Thin state providers**
   - Keep Groovy/Java page implementations focused on:
     - Reading/writing settings and calling services.
     - Building initial state objects.
     - Implementing actions and streams.
   - Avoid embedding layout logic or complex view conditionals in code; put them in the schema instead.

3. **Declarative-first React**
   - Prefer:
     - `DeclarativeExtensionView` + JSON schema + backend handlers.
   - Avoid:
     - Feature-specific React components with embedded logic, unless:
       - An experiment or temporary tool.
       - A generic component candidate that will be integrated into `componentLibrary`.

4. **Use streams for live data**
   - For anything that needs real-time updates (logs, network packets, metrics, health), prefer:
     - A single `streamId` per logical data source.
     - Snapshot + `scan` pattern on the backend.
     - Templated `streamParams` + dynamic re-subscription on the frontend.

5. **Use settings providers and configs**
   - For configuration-heavy pages (training, healing, navigation, inventory, skills):
     - Surface configuration via `ISettingsRegistry` providers.
     - Let the page edit the declarative config object.
     - Let the engine/runtime consume that config, not UI-specific state.

---

### How to add a new declarative page

1. **Define the schema**
   - Create a JSON schema describing layout, bindings, and actions.
   - Use the existing schemas (`Log.json`, `Training.json`, etc.) as references.

2. **Create a state provider**
   - For machine-scoped pages:
     - Implement an `IScriptedPage` Groovy class under `scripts/pages/` and wire it via `MachinePagesActivator` / `ScriptPageLoader`.
   - For global/group/system pages:
     - Implement a Java OSGi component (e.g. `*PageRegistrar`) that:
       - Loads the JSON schema.
       - Registers it via `IWebviewConfigurator.addDeclarativePage`.
       - Registers schema, action, and stream handlers.

3. **Wire actions**
   - Add `onClick`, `onChange`, or other event props in the schema as string action names.
   - Implement the corresponding backend handler (Groovy or Java) that:
     - Updates settings or state.
     - Returns either:
       - `state` – new state object to merge.
       - `delta` – partial state update for high-frequency scenarios.

4. **Wire streams (if needed)**
   - In the schema:
     - Add `streamId`, `stateKey`, and optionally `streamParams`.
     - If using filters, express them via template expressions (e.g. `"${Filter.level}"`).
   - In the backend:
     - Register a stream handler that:
       - Builds an initial snapshot state.
       - Composes a `Flux` (`Flux.concat(initial, live)`).
       - Uses `scan` or similar to maintain a bounded state (if stream emits individual events).

5. **Test end-to-end**
   - Verify:
     - Schema loads via `extension.schema`.
     - Actions are invoked and state updates propagate.
     - Streams connect and react to filter changes.

This model is now used for:

- Machine log pages (per-bot logs, with level and feature filters).
- Group and system log pages.
- Packet monitoring.
- Training, healing, navigation, inventory, skills, environment, and connection pages (already partially declarative, to be further aligned with this model).

Future work will focus on:

- Ensuring all new UI surfaces follow this pattern by default.
- Migrating any remaining imperative pages to declarative schemas and state providers.

