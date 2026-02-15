# Prevention Guide: Avoiding Regression & Ensuring Stability

This guide consolidates lessons learned from debugging duplicated machine pages and failed extensions. Follow these practices to prevent recurrence of similar issues.

## 1. Robust Frontend State Management

### Problem: Duplicated UI Elements
**Issue:** Using `Array.filter` with loose string matching (like `.includes()`) can cause over-matching, where one entity's ID matches another's (e.g., "TEST" matches "TESTA" or "ASDASDASD").
**Prevention:**
- **Strict Matching:** Always use strict equality (`===`) or precise suffix checks (`.endsWith('_' + id)`) when filtering by ID.
- **Normalized State:** Store collections as Maps/Records (`Record<string, Entity>`) keyed by unique ID rather than Arrays. This naturally dedoubles updates if the same ID is received multiple times.
- **Unique ID Generation:** Ensure backend IDs are globally unique and follow a strict pattern (e.g., `${type}_${machineId}`).

```typescript
// BAD: Loose matching
items.filter(item => item.id.includes(machineId))

// GOOD: Strict matching
items.filter(item => item.id === machineId || item.id.endsWith(`_${machineId}`))
```

## 2. OSGi Service Best Practices

### Problem: `UnsatisfiedReference` & `ClassCastException`
**Issue:** Injecting a concrete implementation class (e.g., `WebviewConfigurator`) instead of its interface (e.g., `IWebviewConfigurator`) causes:
1.  **Runtime failures** if the implementation class is not exported (it shouldn't be!).
2.  **Proxy errors** because OSGi services are often proxies that strictly implement the interface.

**Prevention:**
- **Always Reference Interfaces:** in `@Reference` annotations, always use the service interface.
- **Export API, Hide Impl:** Ensure your bundle exports `*.api` packages but keeps `*.internal` or `*.impl` private.

```java
// BAD
@Reference
private WebviewConfigurator configurator; // Concrete class

// GOOD
@Reference
private IWebviewConfigurator configurator; // Interface
```

## 3. Handling Lifecycle Race Conditions

### Problem: Services Starting "Too Early"
**Issue:** `MachinePagesActivator` tried to register pages before the `SettingsRegistry` had the necessary scopes ("training", "login") registered by other bundles.
**Prevention:**
- **Global Registrars:** If a foundational scope/service is needed by many components, register it in a dedicated "Registrar" component that starts immediately, rather than lazily in a consumer.
- **Service Dependencies:** Use `@Reference` to block your component start until dependencies are ready. If checks are complex, use `EventAdmin` to listen for "Ready" events (e.g., `TOPIC_MACHINE_CONTEXT_CREATED` is good, but ensure *global* prerequisites are also met).

## 4. Explicit Bundle Configuration

### Problem: Bundle Identity Collisions
**Issue:** Default Maven behavior resulted in confused bundle symbolic names.

**Prevention:**
- **Explicit Symbolic Names:** Always define `<Bundle-SymbolicName>` in `pom.xml`.
- **Feature Manifests:** Immediately add new bundles to `feature.xml` to ensure they are installed in a deterministic order.

## 5. Defensive Coding in Handlers

### Problem: Silent Failures
**Issue:** `ExtensionHandler` was silently failing or throwing generic errors without context when dependencies were missing.

**Prevention:**
- **Null Checks:** Explicitly check for null services in handlers.
- **Meaningful Logs:** Log *why* a request is rejected (e.g., "WebviewConfigurator service not available").
- **Timeouts:** in RSocket or other async handlers, ensure timeouts are set so the UI doesn't hang indefinitely.
