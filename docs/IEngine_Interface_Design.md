# IEngine Interface Design - Framework-Agnostic API

## Design Principles

### 1. Framework Independence
- ✅ **API layer should NOT depend on Spring**
- ✅ No Spring-specific types in public interfaces
- ✅ Framework details hidden in implementation

### 2. Encapsulation
- ✅ `IPacketPublisher` accessed via `IMachineContext.packetPublisher()`
- ✅ State machine access (if needed) through context, not engine directly
- ✅ Engine interface focuses on lifecycle only

## IEngine Interface (Final)

```java
package org.sokybot.engine;

/**
 * Public interface for a machine engine instance.
 * Framework-agnostic - no Spring dependencies.
 */
public interface IEngine {
    
    /**
     * Gets the machine ID this engine belongs to.
     * Format: "groupName.machineName"
     */
    String getMachineId();
    
    /**
     * Starts the engine (state machine, actuators, etc.).
     */
    void start();
    
    /**
     * Stops the engine and releases resources.
     */
    void stop();
    
    /**
     * Checks if the engine is currently running.
     */
    boolean isRunning();
}
```

## What's NOT in IEngine (and why)

### ❌ No `getStateMachine()`
**Reason:** Would expose Spring `StateMachine<MachineState, IMachineEvent>` type
- Makes API depend on Spring framework
- Violates framework independence principle

**Alternative:** If plugins need state machine access, it should be:
- Through `IMachineContext` (if needed)
- Or via abstraction/facade that doesn't expose Spring types
- Or plugins work with game events (EventAdmin) instead of direct state machine

### ❌ No `getPacketPublisher()`
**Reason:** Better encapsulation - accessed through context
- `IMachineContext.packetPublisher()` is the correct access point
- Keeps engine interface minimal
- Follows single responsibility

**Usage:**
```java
// ✅ Correct
IPacketPublisher publisher = machineContext.packetPublisher();

// ❌ Wrong
IPacketPublisher publisher = engine.getPacketPublisher();
```

## Component Access Pattern

### For Plugins

```java
@Component(
    service = EventHandler.class,
    property = {
        EventConstants.EVENT_TOPIC + "=" + ContextLifecycleEvents.TOPIC_MACHINE_CONTEXT_CREATED
    }
)
public class MyPlugin implements EventHandler {
    
    @Override
    public void handleEvent(Event event) {
        IMachineContext context = (IMachineContext) 
            event.getProperty(ContextLifecycleEvents.PROP_CONTEXT);
        
        // Access packet publisher through context
        IPacketPublisher publisher = context.packetPublisher();
        
        // Access engine if needed (for lifecycle only)
        // Note: IMachineContext may expose getEngine() if needed
        // IEngine engine = context.getEngine();
        
        // Plugin installation logic
        // - Subscribe to game events via EventAdmin
        // - Register UI components via context.machinePageViewer()
        // - Access other services via context
    }
}
```

### Component Assembly

```java
// Factory assembles machine components
public IMachineContext assembleMachine(...) {
    // 1. Create proxy
    IProxyConnection proxy = proxyFactory.createConnection(machineId, listener);
    
    // 2. Create engine (engine internally gets packet publisher from proxy)
    IEngine engine = engineFactory.createEngine(machineId, proxy, groupName, machineName);
    
    // 3. Create context (wraps both)
    IMachineContext context = new MachineContextImpl(machineInfo, proxy, engine);
    // Context provides:
    //   - packetPublisher() → from proxy
    //   - getEngine() → if needed (optional)
    
    return context;
}
```

## Benefits

1. **Framework Independence**
   - API doesn't depend on Spring
   - Can swap implementation frameworks if needed
   - Clean separation of concerns

2. **Better Encapsulation**
   - Components accessed through proper channels
   - Context is the facade for machine components
   - Engine is just one component

3. **Plugin-Friendly**
   - Plugins work with context, not implementation details
   - Event-driven architecture (EventAdmin)
   - No direct framework dependencies

4. **Minimal Interface**
   - Only essential lifecycle methods
   - Easy to implement
   - Easy to test

## Migration Notes

When implementing `IEngineFactory`:
- Engine implementation can still use Spring internally
- Just don't expose Spring types in the interface
- Wrap Spring StateMachine if needed, but don't expose it

When implementing `IMachineContext`:
- `packetPublisher()` returns from proxy connection
- Optionally expose `getEngine()` if plugins need it
- Context acts as facade for all machine components
