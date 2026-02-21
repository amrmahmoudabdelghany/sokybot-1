---
name: UI Page Development
description: How to develop React frontend components and Scripted UI Pages.
---

# UI Page Development

Bot UI is divided into the core Javascript frontend (React) and the Scripted Pages (Groovy + JSON) that define specific bot views.

## 1. Development Environment

Always use the `soky` CLI to run the frontend development servers. This proxies requests to the backend (`localhost:8182` via RSocket).

```bash
# Starts Webview on 5173, DevTools on 3000
soky ui dev
```
Do not restart backend services when only making frontend structural changes.

## 2. Declarative UI (Scripted Pages)

Scripted pages allow you to define bot machine UI dynamically using JSON and Groovy without rebuilding Java modules. This is the preferred way to add new UI for specific bot logic.

### A. Create Schema File
Define the component tree using standard JSON schema in `scripts/pages/{PageName}.json`. Use standard TailwindCSS classes in the `className` prop.

```json
{
  "type": "div",
  "className": "p-4",
  "children": [
    {
      "type": "h3",
      "props": { "children": "My New Page" }
    },
    {
      "type": "Button",
      "props": { "onClick": "myAction", "children": "Click Me" }
    }
  ]
}
```

### B. Create Logic Script
Create the backing logic in `scripts/pages/{PageName}.groovy`.

```groovy
import org.sokybot.machinepages.api.IScriptedPage
import org.sokybot.runtime.IMachineContext
import reactor.core.publisher.Flux

class MyPage implements IScriptedPage {
    String getTitle() { "My Page" }
    String getIcon() { "Layout" } // Lucide icon name
    
    void init(IMachineContext context) {}
    
    Map<String, Object> getInitialState() { return [ count: 0 ] }
    
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        if (action == "myAction") { /* process */ }
        return [:]
    }

    Flux<Object> streamData(String name, Map<String, Object> params) {
        return Flux.empty()
    }

    void shutdown() {}
}

// CRITICAL: Return instance
new MyPage()
```

### C. Hot-Reload and Verify via CLI

When developing scripted pages, do **not** restart the backend.
1. Save the `.json` and `.groovy` files.
2. In the `soky backend shell`, force an explicit reload if it doesn't automatically reflect:
   ```bash
   karaf@root()> dev:script-reload
   ```
3. Check `soky backend logs` for any Groovy compilation or JSON parsing errors.

## 3. Frontend Technology Stack

When working directly in the React frontend (`ui/` workspace):
- **Framework**: React 19 + TypeScript.
- **Styling**: TailwindCSS (Utility-first). No raw CSS files.
- **Components**: Use Radix UI Primitives and fetch shared components from `@sokybot/frontend-shared`.
- **Communication**: Frontend communicates with Java backend ONLY via RSocket (WebSocket). **Do NOT use REST or HTTP fetch**.

### RSocket Format

```typescript
import { rsocketService } from './RSocketClient';

// Generic request-response
const result = await rsocketService.request<MyType>('my.method', { param1: 'value' });

// Request-stream (real-time updates)
const subscription = rsocketService.subscribe<EventType>(
    'game.events',
    (event) => console.log('Received:', event),
    (error) => console.error('Error:', error),
    { machineId: 'optional-filter' }
);
subscription.unsubscribe();
```
