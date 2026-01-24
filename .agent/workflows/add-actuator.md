---
description: How to add a new Actuator (Bot Logic)
---

# How to Add a New Actuator

Actuators are the core logic units of the bot (e.g., specific grinding behavior, event handlers).

## 1. Create Module (Optional)
If the functionality is large, create a new Maven module `sokybot-actuator-{name}`.
Otherwise, add to an existing relevant actuator bundle.

## 2. Define Dependencies
Ensure `pom.xml` includes:
```xml
<dependency>
    <groupId>${project.groupId}</groupId>
    <artifactId>sokybot-engine-api</artifactId>
    <version>${project.version}</version>
</dependency>
```

## 3. Implement IActuator
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
}
```

## 4. Define Logic Cycle
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

## 5. Build and Deploy
Run `./mvnw install -pl sokybot-actuator-{name}` and update Karaf features if it's a new bundle.
