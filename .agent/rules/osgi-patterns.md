---
trigger: always_on
glob: "**/*.java"
description: OSGi and Karaf specific patterns and guidelines
---

# OSGi & Karaf Patterns

## 1. Bundle Structure

Each OSGi bundle should follow this structure:

```
sokybot-{module}/
├── pom.xml                    # Bundle configuration
└── src/main/java/org/sokybot/{module}/
    ├── api/                   # Exported interfaces
    │   └── IMyService.java
    ├── internal/              # Private implementation
    │   ├── MyServiceImpl.java
    │   └── MyActivator.java
    └── domain/                # Domain models (if exported)
```

## 2. Bundle Manifest Headers

Standard headers for `maven-bundle-plugin`:

```xml
<instructions>
    <Bundle-SymbolicName>${project.groupId}.${project.artifactId}</Bundle-SymbolicName>
    <Bundle-Name>${project.name}</Bundle-Name>
    <Bundle-Version>${project.version}</Bundle-Version>
    <Bundle-Description>${project.description}</Bundle-Description>
    
    <!-- Only export API packages -->
    <Export-Package>org.sokybot.{module}.api.*</Export-Package>
    
    <!-- Keep implementation private -->
    <Private-Package>org.sokybot.{module}.internal.*</Private-Package>
    
    <!-- Import with optional resolution -->
    <Import-Package>*;resolution:=optional</Import-Package>
</instructions>
```

## 3. Declarative Services (DS)

### Service Component

```java
@Component(
    service = IMyService.class,
    immediate = true
)
public class MyServiceImpl implements IMyService {
    
    @Reference
    private IDependency dependency;
    
    @Activate
    protected void activate(ComponentContext context) {
        // Initialization
    }
    
    @Deactivate
    protected void deactivate() {
        // Cleanup
    }
}
```

### Reference Cardinality

| Cardinality | Meaning |
|-------------|---------|
| `@Reference` (default) | Mandatory 1..1 |
| `@Reference(cardinality = OPTIONAL)` | Optional 0..1 |
| `@Reference(cardinality = MULTIPLE)` | List 0..n |
| `@Reference(cardinality = AT_LEAST_ONE)` | Required 1..n |

### Reference Policy

| Policy | Meaning |
|--------|---------|
| `STATIC` (default) | Component restarts if reference changes |
| `DYNAMIC` | Reference updated without restart |

## 4. Bundle Activator (Legacy)

Use DS components instead when possible, but for legacy:

```java
public class MyActivator implements BundleActivator {
    
    @Override
    public void start(BundleContext context) throws Exception {
        // Register services
        context.registerService(IMyService.class, new MyServiceImpl(), null);
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        // Cleanup (services auto-unregistered)
    }
}
```

Configure in pom.xml:
```xml
<Bundle-Activator>org.sokybot.{module}.internal.MyActivator</Bundle-Activator>
```

## 5. Service Lookup Patterns

### Preferred: Declarative Services
```java
@Reference
private IGameLoader gameLoader;
```

### Alternative: ServiceTracker
```java
ServiceTracker<IGameLoader, IGameLoader> tracker = 
    new ServiceTracker<>(context, IGameLoader.class, null);
tracker.open();
IGameLoader loader = tracker.getService();
```

### Avoid: Direct Lookup
```java
// Only for legacy/special cases
ServiceReference<IGameLoader> ref = context.getServiceReference(IGameLoader.class);
IGameLoader loader = context.getService(ref);
```

## 6. Common OSGi Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| `ClassNotFoundException` | Package not imported | Add to `Import-Package` |
| `NoClassDefFoundError` | Transitive dependency missing | Check bundle dependencies |
| Service not found | Component not activated | Check DS annotations, logs |
| Circular dependency | Bundles depend on each other | Refactor to break cycle |
| Split package | Same package in multiple bundles | Consolidate or rename |

## 7. Karaf Features

Features are defined in `sokybot-features/src/main/feature/feature.xml`.

### Shared Infrastructure Features

The following features provide shared dependencies to avoid duplication:

| Feature | Description | Bundles |
|---------|-------------|---------|
| `sokybot-netty` | Netty networking stack | netty-common, netty-buffer, netty-transport, etc. |
| `sokybot-reactor` | Reactor reactive streams | reactor-core, reactor-netty-core, reactor-netty-http |
| `sokybot-jackson` | Jackson JSON processing | jackson-core, jackson-databind, jackson-annotations |
| `sokybot-rsocket` | RSocket protocol | rsocket-core, rsocket-transport-netty |

### Application Features

| Feature | Description | Dependencies |
|---------|-------------|--------------|
| `sokybot-core` | Core bundles | sokybot-reactor, sokybot-jackson |
| `sokybot-actuators` | Bot automation | sokybot-core |
| `sokybot-dev` | Development tools | sokybot-actuators, shell, ssh |
| `sokybot-full` | Production app | sokybot-actuators |

### Feature Definition Example

```xml
<feature name="sokybot-core" version="${project.version}">
    <!-- Depend on shared infrastructure -->
    <feature>sokybot-reactor</feature>
    <feature>sokybot-jackson</feature>
    <!-- Application bundles -->
    <bundle>mvn:io.github.sokybot/sokybot-commons/${project.version}</bundle>
    <bundle>mvn:io.github.sokybot/sokybot-http-server/${project.version}</bundle>
</feature>
```

## 8. Dependency Strategy: Features vs Embedding

### Default: Use Karaf Features (Preferred)

For OSGi-compatible shared libraries (Netty, Reactor, Jackson), use features:

```xml
<!-- In pom.xml: declare as provided -->
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-core</artifactId>
    <scope>provided</scope>
</dependency>

<!-- In bundle instructions: import packages -->
<Import-Package>
    reactor.core.*;version="[3.5,4)",
    *
</Import-Package>
<Embed-Dependency>!*</Embed-Dependency>
```

**Benefits:**
- Smaller bundle sizes
- Single classloader for shared libs
- Proper OSGi service identity
- Efficient memory usage
- Easy version updates

### When to Embed

Only embed for:
1. **Non-OSGi JARs** that can't be wrapped (proprietary libs)
2. **Problematic libraries** with OSGi issues (Hibernate, reflection-heavy)
3. **Small module-specific utilities** (commons-io in pk2-extractor)

```xml
<Embed-Dependency>hibernate-core,hibernate-osgi</Embed-Dependency>
<Embed-Transitive>true</Embed-Transitive>
```

**Warning**: Embedding increases bundle size and can cause classloading issues.

## 9. Configuration Admin

For configurable services:

```java
@Component(
    configurationPid = "org.sokybot.myservice",
    configurationPolicy = ConfigurationPolicy.OPTIONAL
)
public class MyConfigurableService {
    
    @Activate
    protected void activate(Map<String, Object> config) {
        String value = (String) config.get("my.property");
    }
}
```

Config file: `etc/org.sokybot.myservice.cfg`

## 10. Standardization Checklist
1. **Bundle-SymbolicName**: Must be `${project.groupId}.${project.artifactId}`
2. **Bundle-Name**: Must be `${project.name}` (human readable)
3. **Bundle-Version**: Must be `${project.version}`
4. **Bundle-Description**: Must be `${project.description}`

## 11. Best Practices

1. **Export only API packages** - Keep implementation private
2. **Use DS over Activators** - Cleaner lifecycle management
3. **Avoid static state** - OSGi services should be stateless or properly scoped
4. **Handle service dynamics** - Services can come and go
5. **Version packages** - Use semantic versioning for exported packages
6. **Test in OSGi** - Unit tests may miss OSGi-specific issues
