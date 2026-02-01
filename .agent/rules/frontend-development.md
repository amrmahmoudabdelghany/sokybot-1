---
trigger: always_on
glob: "**/sokybot-webview/src/main/frontend/**/*"
description: Guidelines for React/TypeScript frontend development in webview
---

# Frontend Development Rules

## 1. Technology Stack
- **Build System**: Vite (via `frontend-maven-plugin`).
- **Framework**: React 19 + TypeScript.
- **Styling**: TailwindCSS (Utility-first).
- **Component Library**: Radix UI Primitives (headless, accessible).
- **Icons**: Lucide React.
- **State/Communication**: RSocket over WebSocket.
- **Workspaces**: NPM Workspaces managed in `ui/`.

## 2. Directory Structure
Frontend code is organized into a monorepo structure under `ui/`:

```
ui/
├── package.json                 # Root workspace config (scripts, dependencies)
├── sokybot-frontend-shared/     # Shared library (@sokybot/frontend-shared)
│   ├── src/lib/                 # Shared utils (cn)
│   ├── src/components/ui/       # Shared UI components
│   └── tailwind-preset.js       # Shared Tailwind configuration
├── sokybot-webview/             # Main Webview Application
│   └── src/main/frontend/       # Frontend code
└── sokybot-dev-tools/           # Developer Tools Application
    └── src/main/frontend/       # Frontend code
```

## 3. Development Workflow
To start both applications (Webview: 5173, DevTools: 3000) simultaneously:

```bash
# Start both Webview (5173) and DevTools (3000)
soky ui dev
```

Dependencies common to all projects (React, Vite, TypeScript) are hoisted to the root `ui/node_modules`.

## 3. Communication Patterns (RSocket)
The frontend communicates with the Java backend ONLY via RSocket.

**Do NOT use**: REST, HTTP fetch, or raw WebSockets directly.

### Protocol Format
Requests use a structured JSON format with `method` and `params`:

```typescript
// Request format
{ "method": "group.list", "params": { "filter": "active" }, "id": "optional-correlation-id" }

// Response format
{ "result": { ... }, "id": "correlation-id" }       // Success
{ "error": { "code": -32601, "message": "..." } }   // Error
```

### Usage
Use the `rsocketService` singleton with typed methods:

```typescript
import { rsocketService } from './RSocketClient';

// Preferred: Use typed convenience methods
const groups = await rsocketService.getGroups();
const machines = await rsocketService.getMachines();
const state = await rsocketService.getCharacterState(machineId);

// Generic request-response
const result = await rsocketService.request<MyType>('my.method', { param1: 'value' });

// Request-stream (for real-time updates)
const subscription = rsocketService.subscribe<EventType>(
    'game.events',
    (event) => console.log('Received:', event),
    (error) => console.error('Error:', error),
    { machineId: 'optional-filter' }
);

// Cleanup
subscription.unsubscribe();
```

### Available Methods
Common RSocket methods (see `SystemInfoHandler` for full list):

| Method | Description |
|--------|-------------|
| `group.list` | List all groups |
| `machine.list` | List all machines |
| `machine.start` | Start a machine |
| `machine.stop` | Stop a machine |
| `character.state` | Get character state |
| `fs.list` | List directory contents |
| `fs.roots` | Get filesystem roots |
| `extension.registry` | Get registered extensions |
| `system.methods` | List all available methods |
| `metrics.stream` | Real-time system/game metrics |
| `script.load` | Load Groovy bot script |
| `events.stream` | Historical/Real-time event journal |
| `health.check` | System health status |

## 4. Component Guidelines
1. **Functional Components**: Use React Functional Components with Hooks.
2. **Strict Types**: Always define props interfaces.
3. **Tailwind First**: Use Tailwind classes for styling. Avoid CSS files.
4. **Radix Primitives**: Build complex interactive components (Dialogs, Popovers) using Radix UI primitives for accessibility.
5. **Shared Components**: Always import base UI components from `@sokybot/frontend-shared` instead of creating local copies.
