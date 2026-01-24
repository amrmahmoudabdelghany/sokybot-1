---
trigger: always_on
glob: "**/sokybot-actuator-*/**/*"
description: Bot logic and actuator development guidelines
---

# Bot Logic (Actuator) Rules

## 1. Actuator Structure
Bot logic is encapsulated in **Actuators**.
- **Interface**: Implement `IActuator`.
- **Registration**: Use `@Component(service = IActuator.class)`.

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
- `IWorkflowContext`: Runtime context (dispatcher, game model, settings).

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
