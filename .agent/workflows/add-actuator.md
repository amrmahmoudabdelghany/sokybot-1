---
description: How to add a new Actuator (Bot Logic)
---

# How to Add a New Actuator

Actuators are the core logic units of the bot (e.g., specific grinding behavior, event handlers).

## Option 1: Groovy Script (Preferred/Fast Iteration)
Use this for most bot logic. Supports **Hot-Reloading** and simplifies dependency management.

### 1. Create Script File
Create a new file in `scripts/actuators/{name}.groovy`.

### 2. Implement Logic
The script must define a class implementing `IActuator` and return an instance of it.

```groovy
import org.sokybot.engine.api.extension.IActuator
import org.sokybot.engine.api.extension.IActuatorContext
import org.sokybot.engine.api.extension.ActuatorDescriptor
import org.sokybot.game.model.api.IGameModel

class MyScriptActuator implements IActuator {
    @Override String getName() { "my-script" }
    @Override void initialize(IActuatorContext context) {
         // Retrieve services via getService
         def gameModel = context.getService(IGameModel)
         
         // Define cycles here
    }
    @Override void shutdown(IActuatorContext context) {}
    @Override ActuatorDescriptor getDescriptor() {
        ActuatorDescriptor.builder(getName()).displayName("My Script").build()
    }
}

// CRITICAL: Return the instance
new MyScriptActuator()
```

### 3. Hot-Reload
Simply save the file. The server detects the change, stops the old instance, and starts the new one.

---

## Option 2: Java Bundle (Legacy/Complex)
Use this only for extremely complex logic that requires heavy unit testing or multi-bundle orchestration.

### 1. Create Module
Create a new Maven module `sokybot-actuator-{name}` under `legacy/` (or a relevant directory). Register it in the parent POM.

### 2. Implement and Register
Implement `IActuator` and register as an OSGi component:
```java
@Component(service = IActuator.class)
public class MyActuator implements IActuator { ... }
```

### 3. Deploy
Build and install into Karaf. Update the `sokybot-features` if it's a new bundle.
