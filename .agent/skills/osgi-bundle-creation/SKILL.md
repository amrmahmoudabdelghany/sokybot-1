---
name: OSGi Bundle Creation
description: How to create, configure, and deploy a new OSGi Bundle.
---

# OSGi Bundle Creation

## 1. Create Directory Structure
Choose a category (`core`, `game`, `ui`, `network`, `infra`) and create the module inside it:

```
{category}/sokybot-{name}/
├── src/main/java/org/sokybot/{name}/
│   ├── api/       # Exported interfaces
│   └── internal/  # Private implementation
└── pom.xml
```

## 2. Configure POM
Use the standard parent with relative path `../../pom.xml`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xsi:schemaLocation="...">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.sokybot</groupId>
        <artifactId>sokybot</artifactId> <!-- Or category aggregator -->
        <version>1.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>sokybot-{name}</artifactId>
    <packaging>bundle</packaging>
    <name>Sokybot {Name}</name>
    <description>Description of the bundle</description>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-bundle-plugin</artifactId>
                <extensions>true</extensions>
                <configuration>
                    <instructions>
                        <!-- REQUIRED CRITICAL RULE -->
                        <Bundle-SymbolicName>${project.groupId}.${project.artifactId}</Bundle-SymbolicName>
                        <Export-Package>org.sokybot.{name}.api.*</Export-Package>
                        <Private-Package>org.sokybot.{name}.internal.*</Private-Package>
                    </instructions>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## 3. Register in POMs and Features
1. Add to the `<modules>` section of the parent `{category}/pom.xml`.
2. Add the bundle to `infra/sokybot-features/src/main/feature/feature.xml` IMMEDIATELY.

## 4. Component Definition (Declarative Services)
**CRITICAL RULE**: Do NOT use `bundleContext.registerService()` in Activators. Use Declarative Services (`@Component`) exclusively.

```java
@Component(service = IMyService.class, immediate = true)
public class MyServiceImpl implements IMyService {
    @Reference
    private IDependency dependency;
}
```

## 5. Build and Deploy

Use the `soky` CLI for building and deploying. This is significantly faster than a full maven install and prevents cache issues.

```bash
# Safest: Build, clear Karaf cache, and restart backend
soky deploy sokybot-{name}

# Fastest (Pure logic updates only): Build and hot-reload via Karaf
soky deploy sokybot-{name} --no-restart
```

## 6. Critical Prevention Rules
1. **Explicit Bundle-SymbolicName**: Must define `<Bundle-SymbolicName>${project.groupId}.${project.artifactId}</Bundle-SymbolicName>`.
2. **Feature First**: Add new bundles to `sokybot-features` immediately.
3. **Integration Testing**: Unit tests mock the classpath; they do NOT verify runtime OSGi resolution. Verify Import-Package headers.
