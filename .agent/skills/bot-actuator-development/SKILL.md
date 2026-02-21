---
name: Bot Actuator Development
description: How to develop, test, and hot-reload bot logic (Actuators).
---

# Bot Actuator Development

Actuators are the core logic units of the bot (e.g., specific grinding behavior, event handlers). They operate on a State Machine (Cycles) pattern and should be reactive to `IGameModel` state changes rather than raw packets.

## 1. Actuator Structure (Groovy Preferred)

For most bot logic, use Groovy scripts to enable **Hot-Reloading** and fast iteration.

Create a new file in `scripts/actuators/{name}.groovy`.

```groovy
import org.sokybot.engine.api.extension.IActuator
import org.sokybot.engine.api.extension.IActuatorContext
import org.sokybot.engine.api.extension.ActuatorDescriptor
import org.sokybot.gamemodel.IGameModel

class MyScriptActuator implements IActuator {
    @Override String getName() { "my-script" }
    
    @Override void initialize(IActuatorContext context) {
         // Retrieve services via getService
         def gameModel = context.getService(IGameModel.class)
         
         // Define cycles here using CycleDefinitionBuilder
    }
    
    @Override void shutdown(IActuatorContext context) {}
    
    @Override ActuatorDescriptor getDescriptor() {
        ActuatorDescriptor.builder(getName()).displayName("My Script").build()
    }
}

// CRITICAL: Return the instance
new MyScriptActuator()
```

## 2. State Machine (Cycles)
Logic flow is defined using **Cycles** (State Machines).
- **Builder**: Use `CycleDefinitionBuilder`.
- **States**: Define `entryState`, intermediate `state` nodes, and transitions.
- **Components**:
  - `Guard`: Condition to enter a state (`ctx -> boolean`).
  - `Action`: Logic to execute in a state (`ctx -> void`).
  - `Next`: Default transition.
  - `Target`: Transition if guard fails.

### Actuator Isolation Principle
- Actuators **MUST NOT** depend directly on raw events or packets unless necessary for specialized tasks.
- Actuators **MUST** rely on state polled from `IGameModel` and `IGameStateProvider` via context.
- Logic should be reactive to state changes in the model.

## 3. Development Workflow with `sokybot-dev-shell`

**CRITICAL**: Do NOT restart the entire backend to test actuator logic changes. Use the development CLI via `soky backend shell`.

### A. Evaluating Logic Snippets
Before writing a full cycle, test your logic on a running bot:
```bash
soky backend shell
karaf@root()> dev:eval <groupName> <botName> "context.getService(org.sokybot.gamemodel.IGameModel.class).observeAll().blockFirst()"
```

### B. Hot-Reloading Actuators
Once your script is saved, force a reload to apply changes to all bots:
```bash
karaf@root()> dev:script-reload
```
This unloads all scripts, recompiles them, and registers the new instances.

### C. Inspecting Bot State
Verify that your actuator logic is correctly updating or reading the bot's state:
```bash
karaf@root()> dev:inspect <groupName> <botName>
```

## 4. Java Bundle (Legacy/Complex)
Use Java components (`sokybot-actuator-{name}`) *only* for extremely complex logic requiring heavy unit testing multi-bundle orchestration. Implement `IActuator` and register via `@Component(service = IActuator.class)`. Use `soky deploy sokybot-actuator-{name}` to build and hot-reload.
