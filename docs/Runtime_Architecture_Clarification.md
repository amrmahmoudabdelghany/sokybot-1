# Runtime Architecture Clarification

## Understanding the Current Architecture

### Runtime Hierarchy

```
ISokybotContext (ApplicationContextAdapter)
  │
  │ Represents: Entire Runtime (single instance)
  │ Manages: All machine groups
  │
  ├── IGroupContext (GroupContextAdapter) - "Group1"
  │   │
  │   │ Has: Own Spring context (MachineGroupConfig)
  │   │ Manages: All machines in this group
  │   │
  │   ├── IMachineContext (MachineContextAdapter) - "Machine1"
  │   │   │
  │   │   │ Has: Own engine instance (Spring context with MachineConfig)
  │   │   │ Contains: State machine, actuators, controllers
  │   │   │
  │   │   └── Engine Instance
  │   │       ├── State Machine
  │   │       ├── Actuators
  │   │       └── Controllers
  │   │
  │   └── IMachineContext (MachineContextAdapter) - "Machine2"
  │       │
  │       │ Has: Own engine instance (separate from Machine1)
  │       │
  │       └── Engine Instance
  │
  └── IGroupContext (GroupContextAdapter) - "Group2"
      │
      └── IMachineContext (MachineContextAdapter) - "Machine3"
          │
          │ Has: Own engine instance (separate from all others)
          │
          └── Engine Instance
```

### Key Points

1. **ISokybotContext = Entire Runtime**
   - Single instance in the application
   - Represents the whole SokyBot runtime
   - Manages all groups and machines
   - Implemented by `ApplicationContextAdapter` (Spring bean)

2. **Each Machine Has Its Own Engine Instance**
   - Each `IMachineContext` (MachineContextAdapter) creates its own Spring context
   - This Spring context uses `MachineConfig` which contains:
     - State machine configuration
     - Actuators
     - Controllers
     - Machine-specific services
   - Machines are **completely independent** - each has its own isolated engine

3. **Group Contexts Are Intermediate**
   - `IGroupContext` (GroupContextAdapter) has its own Spring context
   - Uses `MachineGroupConfig`
   - Manages multiple machines
   - Provides shared services for machines in the group

## Factory Bundle Role

### What Factory Bundle Does

The `sokybot-context-factory` bundle:

1. **Wraps ISokybotContext Service**
   - Intercepts calls to `installGroup()` and `installMachine()`
   - Does NOT replace the runtime - it enhances it

2. **Delegates to Engine**
   - All actual context creation still happens in engine
   - Factory just adds event publishing layer

3. **Publishes Events**
   - When group is created → `GROUP_CONTEXT_CREATED` event
   - When machine is created → `MACHINE_CONTEXT_CREATED` event
   - When destroyed → corresponding `DESTROYED` events

4. **Preserves Engine Instances**
   - Each machine still gets its own engine instance
   - Factory doesn't change how contexts are created
   - Factory only adds event publishing

### Architecture with Factory

```
┌─────────────────────────────────────────────────────────────┐
│          ISokybotContext (from engine)                      │
│          = Entire Runtime                                   │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  ApplicationContextAdapter                          │  │
│  │  - Manages all groups                               │  │
│  │  - Creates GroupContextAdapter instances            │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                           │
                           │ Wrapped by
                           │
┌───────────────────────────▼─────────────────────────────────┐
│          ContextFactoryManager (factory bundle)             │
│  - Intercepts installGroup() / installMachine()            │
│  - Delegates to ISokybotContext                            │
│  - Publishes lifecycle events via EventAdmin               │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            │ Events
                            │
┌───────────────────────────▼─────────────────────────────────┐
│          Machine UI Bundle                                  │
│  - Listens to lifecycle events                             │
│  - Creates UI for each machine (each has own engine)       │
└─────────────────────────────────────────────────────────────┘

Each Machine Still Has Its Own Engine Instance:
┌─────────────────────────────────────────────────────────────┐
│  IMachineContext                                            │
│  └── Spring Context (MachineConfig)                        │
│      ├── State Machine                                      │
│      ├── Actuators                                          │
│      └── Controllers                                        │
└─────────────────────────────────────────────────────────────┘
```

## Implementation Strategy

### Factory Implementation Pattern

```java
@Component(service = ISokybotContext.class)
public class ContextFactoryWrapper implements ISokybotContext {
    
    @Reference
    private ISokybotContext delegate; // From engine
    
    @Reference
    private EventAdmin eventAdmin;
    
    @Override
    public void installGroup(String name, String gamePath, String... options) {
        // Delegate to engine (actual creation)
        delegate.installGroup(name, gamePath, options);
        
        // Get created context
        IGroupContext groupCtx = delegate.findGroupCtx(name).orElseThrow();
        
        // Publish event
        publishGroupCreated(groupCtx);
    }
    
    @Override
    public IGroupContext[] getGroups() {
        return delegate.getGroups(); // Just delegate
    }
    
    // ... other methods delegate to engine
    
    private void publishGroupCreated(IGroupContext context) {
        // Publish event via EventAdmin
    }
}
```

**Note**: For machine creation, we need to intercept at `IGroupContext.installMachine()` level too. This might require:
- Wrapping `IGroupContext` instances as well, OR
- Factory listens to `IGroupListener` / `IMachineListener` events and publishes OSGi events

### Alternative: Factory Listens to Existing Listeners

Since `IGroupContext` already has `IGroupListener` and `IMachineListener`, the factory could:

1. Register itself as a listener on `ISokybotContext`
2. When groups are created → publish event
3. Register as listener on each `IGroupContext`
4. When machines are created → publish event

This is simpler and requires no wrapping!

## Summary

- **ISokybotContext** = Entire runtime (single instance)
- **Each Machine** = Has its own engine instance (isolated Spring context)
- **Factory Bundle** = Adds event publishing layer (wraps or listens)
- **No Changes** = To how contexts/engines are created
- **Benefits** = Reactive UI creation via events, while preserving existing architecture
