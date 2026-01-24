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
cd ui
npm run dev
```

Dependencies common to all projects (React, Vite, TypeScript) are hoisted to the root `ui/node_modules`.

## 3. Communication Patterns (RSocket)
The frontend communicates with the Java backend ONLY via RSocket.

**Do NOT use**: REST, HTTP fetch, or raw WebSockets directly.

### Usage
Use the `rsocketService` singleton:

```typescript
import { rsocketService } from './RSocketClient';

// Request-Response (Single response)
const result = await rsocketService.requestResponse(JSON.stringify({
    command: 'myCommand',
    payload: { ... }
}));

// Request-Stream (Streaming updates)
rsocketService.requestStream(
    JSON.stringify({ command: 'monitorEvents' }),
    (data) => console.log('Received:', data),
    (error) => console.error('Error:', error)
);
```

## 4. Component Guidelines
1. **Functional Components**: Use React Functional Components with Hooks.
2. **Strict Types**: Always define props interfaces.
3. **Tailwind First**: Use Tailwind classes for styling. Avoid CSS files.
4. **Radix Primitives**: Build complex interactive components (Dialogs, Popovers) using Radix UI primitives for accessibility.
5. **Shared Components**: Always import base UI components from `@sokybot/frontend-shared` instead of creating local copies.
