---
description: How to add a new Scripted UI Page (Declarative UI)
---

# How to Add a New Scripted UI Page

Scripted pages allow you to define bot machine UI dynamically using JSON and Groovy without rebuilding Java modules.

## 1. Create Schema File
Create a new file in `scripts/pages/{PageName}.json`. This file defines the React component tree using standard JSON schema.

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
      "props": { 
        "onClick": "myAction",
        "children": "Click Me"
      }
    }
  ]
}
```

## 2. Create Logic Script
Create a new file in `scripts/pages/{PageName}.groovy`. The script must return an instance of `IScriptedPage`.

```groovy
import org.sokybot.machinepages.api.IScriptedPage
import org.sokybot.runtime.IMachineContext
import reactor.core.publisher.Flux

class MyPage implements IScriptedPage {
    String getTitle() { "My Page" }
    String getIcon() { "Layout" } // Lucide icon name
    
    void init(IMachineContext context) {
        // Initialization logic
    }
    
    Map<String, Object> getInitialState() {
        return [ count: 0 ]
    }
    
    Map<String, Object> handleAction(String action, Map<String, Object> data) {
        if (action == "myAction") {
            // Process action
        }
        return [:]
    }

    Flux<Object> streamData(String name, Map<String, Object> params) {
        return Flux.empty()
    }

    void shutdown() {}
}

new MyPage()
```

## 3. Hot-Reload and Verify
1.  **Save files**: The `ScriptPageLoader` will automatically register the new page.
2.  **Open Webview**: Switch to the bot machine view; the new tab should appear immediately.
3.  **Debug**: Check `soky backend logs` for any Groovy compilation or JSON parsing errors.

> [!TIP]
> Use standard TailwindCSS classes in your JSON `className` props for styling.
