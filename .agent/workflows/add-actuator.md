---
description: How to add a new Actuator (Bot Logic)
---

# How to Add a New Actuator

Actuators are the core logic units of the bot (e.g., specific grinding behavior, event handlers).

## Option 1: Java Bundle (Maven)
Use this for complex, stable logic that needs full IDE support and testing.

### 1. Create Module (Optional)
If the functionality is large, create a new Maven module `sokybot-actuator-{name}`.
Otherwise, add to an existing relevant actuator bundle.

### 2. Define Dependencies
Ensure `pom.xml` includes:
```xml
<dependency>
    <groupId>${project.groupId}</groupId>
    <artifactId>sokybot-engine-api</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 3. Actuator Isolation Principle
Before implementing, keep in mind:
- **No Event Bus**: Actuators MUST NOT depend on `sokybot-game-events` or `IReactiveEventBus`.
- **State Polling**: Use `context.getGameModel()` to poll state (HP, MP, Position, Visible entities).
- **Reactive Model**: The model is updated asynchronously via the bus; your cycle simply reads the current snapshot.

### 4. Implement IActuator
Create a class implementing `org.sokybot.engine.api.extension.IActuator`.

```java
@Component(service = IActuator.class, property = {"actuator.name=my-logic"})
public class MyActuator implements IActuator {
    @Override
    public String getName() { return "my-logic"; }
    
    @Override
    public void initialize(IActuatorContext context) {}
    
    @Override
    public void shutdown(IActuatorContext context) {}
    
    @Override
    public ActuatorDescriptor getDescriptor() {
        return ActuatorDescriptor.builder(getName())
            .displayName("My Logic")
            .build();
    }
}
```

### 5. Define Logic Cycle
Inside `initialize()`, define and register your state machine:

```java
ICycleDefinition cycle = new CycleDefinitionBuilder()
    .name("my-cycle")
    .entryState("START")
    .state("START", b -> b
        .guard(ctx -> condition(ctx))
        .action(ctx -> doSomething(ctx))
        .nextState("NEXT_STATE"))
    // ... more states
    .build();

context.getWorkflowRegistry().registerCycle(cycle);
```

### 6. Build and Deploy
Run `./mvnw install -pl sokybot-actuator-{name}` and update Karaf features if it's a new bundle.

---

## Option 2: Groovy Script (Fast Iteration)
Use this for quick prototyping, simple automation, or logic that changes frequently. Supports **Hot-Reloading**.

### 1. Create Script File
Create a new file in `scripts/actuators/{name}.groovy`.

### 2. Implement Logic
The script must define a class implementing `IActuator` and return an instance of it.

```groovy
import org.sokybot.engine.api.extension.IActuator
import org.sokybot.engine.api.extension.IActuatorContext
import org.sokybot.engine.api.extension.ActuatorDescriptor

class MyScriptActuator implements IActuator {
    @Override String getName() { "my-script" }
    @Override void initialize(IActuatorContext context) {
         // Define cycles here just like in Java
    }
    @Override void shutdown(IActuatorContext context) {}
    @Override ActuatorDescriptor getDescriptor() {
        ActuatorDescriptor.builder(getName()).displayName("My Script").build()
    }
}

new MyScriptActuator()
```

### 3. Hot-Reload
Simply save the file. The server will detect the change, stop the old instance, and start the new one automatically. Check logs for compilation errors.
