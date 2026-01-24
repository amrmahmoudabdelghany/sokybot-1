# OSGi Service Access Guide - ISokybotContext

## How ISokybotContext is Registered

The `ISokybotContext` service is registered via **OSGi Declarative Services (DS)**:

### 1. Automatic Registration via DS

`SokybotContextImpl` uses `@Component` annotation:

```java
@Component(immediate = true, service = ISokybotContext.class)
public class SokybotContextImpl implements ISokybotContext {
    @Activate
    public void activate(BundleContext bundleContext) {
        // Service is automatically registered when component is activated
    }
}
```

The `immediate = true` ensures the component is activated immediately when the bundle starts.
The `service = ISokybotContext.class` specifies which service interface to register.

### 2. Manual Registration (Fallback)

`SokybotContextImpl` also manually registers itself in the `@Activate` method:

```java
private void registerOSGiService() {
    if (bundleContext != null) {
        serviceRegistration = bundleContext.registerService(
                ISokybotContext.class,
                this,
                null);
        log.info("ISokybotContext registered as OSGi service");
    }
}
```

This provides a fallback in case DS doesn't work, but DS should handle it automatically.

## How Other Bundles Can Access ISokybotContext

### Method 1: Using Declarative Services (Recommended)

Use `@Reference` annotation in your DS component:

```java
@Component
public class MyBundleComponent {
    
    @Reference
    private ISokybotContext sokytbotContext;
    
    @Activate
    public void activate() {
        // Use sokytbotContext here
        IGroupContext[] groups = sokytbotContext.getGroups();
    }
}
```

**Benefits:**
- Automatic service binding
- Handles service lifecycle (unbind on service removal)
- No manual service lookup needed

### Method 2: Using BundleContext (Manual Lookup)

Get the service reference from BundleContext:

```java
public class MyComponent {
    
    private ISokybotContext getSokybotContext(BundleContext bundleContext) {
        ServiceReference<ISokybotContext> ref = 
            bundleContext.getServiceReference(ISokybotContext.class);
        
        if (ref != null) {
            return bundleContext.getService(ref);
        }
        return null;
    }
}
```

**Note:** Remember to unget the service when done:

```java
bundleContext.ungetService(ref);
```

### Method 3: Using ServiceTracker (For Complex Scenarios)

For tracking service availability:

```java
import org.osgi.util.tracker.ServiceTracker;

public class MyComponent {
    private ServiceTracker<ISokybotContext, ISokybotContext> tracker;
    
    public void start(BundleContext context) {
        tracker = new ServiceTracker<>(context, ISokybotContext.class, null);
        tracker.open();
    }
    
    public ISokybotContext getContext() {
        return tracker.getService();
    }
    
    public void stop() {
        tracker.close();
    }
}
```

## Service Registration Timeline

```
1. Bundle starts
   ↓
2. RuntimeActivator.start() called
   ↓
3. OSGi DS processes @Component annotations
   ↓
4. SokybotContextImpl.activate() called
   ↓
5. Service registered via DS (automatic)
   ↓
6. Service registered manually (fallback)
   ↓
7. Service available to other bundles
```

## Verifying Service Availability

### In Code

```java
BundleContext context = ...;
ServiceReference<ISokybotContext> ref = 
    context.getServiceReference(ISokybotContext.class);

if (ref != null) {
    ISokybotContext service = context.getService(ref);
    log.info("Service available: {}", service.name());
} else {
    log.warn("Service not yet available");
}
```

### Using OSGi Console Commands

If using an OSGi console (like Apache Felix Gogo):

```bash
# List all ISokybotContext services
services | grep ISokybotContext

# Check service properties
scr:list | grep SokybotContextImpl
```

## Troubleshooting

### Service Not Found

1. **Check bundle state:**
   - Ensure `sokybot-runtime` bundle is ACTIVE
   - Check bundle dependencies are resolved

2. **Check DS configuration:**
   - Verify `Service-Component` header in bundle manifest
   - Check DS processor processed annotations correctly

3. **Check service registration:**
   - Look for log messages: "ISokybotContext registered as OSGi service"
   - Verify no exceptions in activate() method

### Service Available but Injection Fails

1. **Check @Reference configuration:**
   - Ensure `@Reference` annotation is present
   - Check if `required = false` (optional dependency)
   - Verify component is DS-enabled

2. **Check bundle dependencies:**
   - Ensure your bundle imports `org.sokybot` package
   - Verify `sokybot-api` bundle is available

## Example: Accessing from Machine UI Bundle

```java
@Component(immediate = true)
public class MachineUIEventHandler implements EventHandler {
    
    @Reference
    private ISokybotContext sokytbotContext;
    
    @Reference
    private EventAdmin eventAdmin;
    
    @Activate
    public void activate() {
        log.info("Machine UI Handler activated");
        log.info("Runtime context: {}", sokytbotContext.name());
        
        // Access groups
        IGroupContext[] groups = sokytbotContext.getGroups();
        for (IGroupContext group : groups) {
            log.info("Found group: {}", group.name());
        }
    }
    
    @Override
    public void handleEvent(Event event) {
        // Handle context lifecycle events
        IMachineContext context = (IMachineContext) 
            event.getProperty(ContextLifecycleEvents.PROP_CONTEXT);
        // ...
    }
}
```

## Bundle Manifest Configuration

Ensure the bundle manifest includes DS headers:

```xml
<Service-Component>
    OSGI-INF/org.sokybot.runtime.SokybotContextImpl.xml
    OSGI-INF/org.sokybot.runtime.GroupContextFactory.xml
    OSGI-INF/org.sokybot.runtime.MachineContextFactory.xml
</Service-Component>
```

These XML files are typically generated by DS annotation processor.
