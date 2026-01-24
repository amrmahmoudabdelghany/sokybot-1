# Context Factory Bundle - Implementation Summary

## Executive Summary

**Decision**: Create `sokybot-context-factory` bundle to manage runtime context lifecycle and publish events. Engine remains focused on machine logic only.

## Architecture Overview

### Runtime Hierarchy
```
ISokybotContext (ApplicationContextAdapter)
  │ = Entire Runtime (single instance)
  │
  ├── IGroupContext (GroupContextAdapter) - Group1
  │   │ = Own Spring context
  │   │
  │   └── IMachineContext (MachineContextAdapter) - Machine1
  │       │ = Own Engine Instance (Spring context with MachineConfig)
  │       └── State Machine, Actuators, Controllers
  │
  └── IGroupContext (GroupContextAdapter) - Group2
      └── IMachineContext (MachineContextAdapter) - Machine2
          │ = Own Engine Instance (separate from Machine1)
          └── State Machine, Actuators, Controllers
```

### Factory Bundle Integration
```
┌─────────────────────────────────────────────────────────────┐
│          ISokybotContext (from engine)                      │
│          = Entire Runtime                                   │
│          - Manages all groups/machines                      │
│          - Each machine has own engine instance             │
└─────────────────────────────────────────────────────────────┘
                           │
                           │ Wrapped/Intercepted by
                           │
┌───────────────────────────▼─────────────────────────────────┐
│          sokybot-context-factory                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ ContextFactoryManager                               │   │
│  │  - Wraps ISokybotContext operations                 │   │
│  │  - Delegates to engine (actual creation)            │   │
│  │  - Publishes lifecycle events via EventAdmin        │   │
│  │  - Listens to IGroupListener/IMachineListener       │   │
│  └─────────────────────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            │ Publishes events via EventAdmin
                            │
┌───────────────────────────▼─────────────────────────────────┐
│          sokybot-machine-ui                                 │
│  - Listens to lifecycle events                              │
│  - Creates UI for each machine                              │
│  - Each machine UI connects to its own engine instance      │
└─────────────────────────────────────────────────────────────┘

Key Point: Each machine still has its own engine instance!
```

## Key Design Decisions

### 1. Factory Bundle Responsibilities

The `sokybot-context-factory` bundle is responsible for:
- ✅ **Wrapping/intercepting** `ISokybotContext` operations
- ✅ **Listening** to context creation via existing listeners
- ✅ **Publishing lifecycle events** via Event Admin
- ✅ **Registering contexts** as OSGi services (optional)
- ✅ **NOT creating contexts** - that's still done by engine

The engine (`ISokybotContext`) is responsible for:
- ✅ Context lifecycle management (creating groups/machines)
- ✅ Managing the entire runtime
- ✅ Creating engine instances for each machine
- ❌ Event publishing (factory handles this)

**Key Understanding**:
- `ISokybotContext` = Entire runtime (single instance)
- Each machine = Has its own engine instance (Spring context with MachineConfig)
- Factory = Adds event publishing layer on top of existing runtime management

### 2. Implementation Strategy

**Recommended**: Factory listens to existing listeners OR wraps `ISokybotContext`

**Option A: Listen to Existing Listeners** (Simpler)
1. Factory registers as `IGroupListener` on `ISokybotContext`
2. When group created → `onGroupInstalled()` → Publish `GROUP_CONTEXT_CREATED` event
3. Factory registers as `IMachineListener` on each `IGroupContext`
4. When machine created → `onMachineInstalled()` → Publish `MACHINE_CONTEXT_CREATED` event

**Option B: Wrap ISokybotContext** (More Control)
1. Factory provides wrapped `ISokybotContext` implementation
2. Intercepts `installGroup()` / `installMachine()` calls
3. Delegates to engine's actual `ISokybotContext`
4. Publishes lifecycle events via Event Admin

**Benefits**:
- Minimal changes to engine
- Preserves existing context creation (each machine gets own engine instance)
- Clean separation: Runtime management (engine) + Event publishing (factory)
- Backward compatible

### 3. Event Publishing

Events published by factory:
- `sokybot/context/group/CREATED` - When group context is created
- `sokybot/context/group/DESTROYED` - When group context is destroyed
- `sokybot/context/machine/CREATED` - When machine context is created
- `sokybot/context/machine/DESTROYED` - When machine context is destroyed

Event properties:
- `groupName` (String)
- `machineName` (String) - for machine events
- `fullName` (String) - "groupName.machineName"
- `context` (IGroupContext or IMachineContext)
- `timestamp` (Long)

## Implementation Plan

### Phase 1: Create Factory Bundle Structure ✅

**Files Created**:
- `sokybot-context-factory/pom.xml`
- Bundle structure and dependencies

**Status**: Structure ready to implement

### Phase 2: Implement Context Factory Manager

**Tasks**:
1. Create `ContextFactoryManager` OSGi DS component
2. Monitor `ISokybotContext` service from engine
3. Implement context creation interception
4. Integrate with Event Admin

**Key Classes**:
- `ContextFactoryManager.java`
- `GroupContextFactory.java`
- `MachineContextFactory.java`

### Phase 3: Event Publishing Integration

**Tasks**:
1. Publish events on context creation
2. Publish events on context destruction
3. Handle error cases with error events
4. Test event flow

### Phase 4: Service Registration (Optional)

**Tasks**:
1. Register `IGroupContext` as OSGi service
2. Register `IMachineContext` as OSGi service
3. Unregister on destruction
4. Add service properties

### Phase 5: Migration & Cleanup

**Tasks**:
1. Remove context lifecycle management from engine
2. Keep context adapter classes in engine (they're just wrappers)
3. Update documentation
4. Test end-to-end

## Files Structure

```
sokybot-context-factory/
├── pom.xml
└── src/main/java/org/sokybot/contextfactory/
    ├── ContextFactoryActivator.java
    ├── ContextFactoryManager.java
    ├── GroupContextFactory.java
    └── MachineContextFactory.java
```

## Dependencies

### Factory Bundle Requires:
- `sokybot-api` - for interfaces
- `sokybot-engine` (provided) - for adapter classes
- `sokybot-persistence` - for repositories
- OSGi Event Admin
- OSGi Declarative Services
- Spring Boot - for context creation

### Engine Bundle Changes:
- Remove Event Admin dependency (no longer needed)
- Keep context adapter classes
- Keep `ISokybotContext` service registration
- Remove context lifecycle management logic

## Benefits

### 1. Clean Architecture
- Engine = machine logic only
- Factory = runtime management
- UI = reactive UI creation
- Clear boundaries

### 2. Separation of Concerns
- Each bundle has single responsibility
- Changes to one don't affect others
- Easier to test and maintain

### 3. Event-Driven
- Loose coupling via events
- Extensible for other bundles
- Reactive architecture

### 4. Maintainability
- Engine code stays focused
- Runtime management isolated
- Easier to add features

## Next Steps

1. **Create factory bundle Maven module**
   - Add to parent `pom.xml`
   - Configure dependencies

2. **Implement `ContextFactoryManager`**
   - OSGi DS component
   - Monitor `ISokybotContext`
   - Intercept context operations

3. **Implement factories**
   - `GroupContextFactory`
   - `MachineContextFactory`
   - Event publishing

4. **Test integration**
   - Create group → verify event
   - Create machine → verify event
   - UI receives events → creates UI

5. **Migrate from engine**
   - Move context lifecycle management
   - Keep adapter classes
   - Remove Event Admin from engine

## Open Questions

1. **Should factory register contexts as OSGi services?**
   - Pros: Other bundles can discover contexts
   - Cons: Duplicate with Spring beans
   - **Recommendation**: Yes, but optional

2. **How to handle existing listeners (IGroupListener, IMachineListener)?**
   - Keep for backward compatibility?
   - Migrate to Event Admin?
   - **Recommendation**: Keep both, deprecate listeners over time

3. **Should factory replace ApplicationContextAdapter entirely?**
   - Current: Factory wraps it
   - Alternative: Factory provides own implementation
   - **Recommendation**: Start with wrapping, evaluate migration later

## References

- `Context_Factory_Bundle_Architecture.md` - Detailed architecture analysis
- `Refactoring_Implementation_Plan.md` - Updated implementation plan
- `Machine_UI_Bundle_Summary.md` - Machine UI bundle details
