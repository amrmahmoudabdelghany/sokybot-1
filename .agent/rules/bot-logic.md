---
trigger: always_on
glob: "{**/sokybot-actuator-*/**/*,scripts/actuators/*.groovy}"
description: Bot logic and actuator development guidelines
---

# Bot Logic (Actuator) Rules

## 1. Actuator Structure
Bot logic is encapsulated in **Actuators**.
- **Interface**: Implement `IActuator`.
- **Registration**: 
  - **Java**: Use `@Component(service = IActuator.class)`. (Legacy)
  - **Groovy**: Place `.groovy` script in `scripts/actuators/`. (Preferred)

## 2. State Machine (Cycles)
Logic flow is defined using **Cycles** (State Machines).
- **Builder**: Use `CycleDefinitionBuilder`.
- **States**: Define `entryState`, intermediate `state` nodes, and transitions.
- **Components**:
  - `Guard`: Condition to enter a state (`ctx -> boolean`).
  - `Action`: Logic to execute in a state (`ctx -> void`).
  - `Next`: Default transition.
  - `Target`: Transition if guard fails.

## 3. Context Access
- `IActuatorContext`: Lifecycle context (machine ID, group name).
  - Use `getService(Class<T>)` to retrieve OSGi services (e.g., `IGameModel`, `INetworkController`).
- `IWorkflowContext`: Runtime context (dispatcher, game model, settings).

### Actuator Isolation Principle
- Actuators **MUST NOT** depend directly on `sokybot-game-events` or `IReactiveEventBus`.
- Actuators **MUST** rely on state polled from `IGameModel` via `IWorkflowContext`.
- Logic should be reactive to state changes in the model, not raw packets or events.

## 4. Example Pattern
```java
ICycleDefinition cycle = new CycleDefinitionBuilder()
    .name("my-logic")
    .entryState("IDLE")
    .state("IDLE", b -> b
        .guard(ctx -> shouldRun(ctx))
        .action(ctx -> performAction(ctx))
        .nextState("FINISHED")
    ).build();
context.getWorkflowRegistry().registerCycle(cycle);
```

## 5. Script-Based Actuators (Groovy)

For fast iteration and hot-reloading, actuators can be written in Groovy.

### Structure
Scripts must be located in `scripts/actuators/*.groovy`.

```groovy
import org.sokybot.engine.api.extension.IActuator
import org.sokybot.engine.api.extension.IActuatorContext
import org.sokybot.engine.api.extension.ActuatorDescriptor

class MyScriptActuator implements IActuator {
    @Override String getName() { "my-script" }
    @Override void initialize(IActuatorContext context) { /* log initialization */ }
    @Override void shutdown(IActuatorContext context) { /* log shutdown */ }
    @Override ActuatorDescriptor getDescriptor() {
        ActuatorDescriptor.builder(getName()).displayName("Groovy Actuator").build()
    }
}

// CRITICAL: The script MUST return an instance of the actuator
new MyScriptActuator()
```

### Hot-Reloading
- Changes to script files are detected automatically via `WatchService`.
- The engine will stop the old instance, re-compile the script, and register the new instance.
- State is NOT preserved across reloads.
