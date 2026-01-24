---
description: How to create a new OSGi Bundle
---

# How to Create a New OSGi Bundle

## 1. Create Directory Structure
Choose a category (e.g., `core`, `game`, `ui`, `network`) and create the module inside it:

```
{category}/sokybot-{name}/
├── src/main/java/org/sokybot/{name}/
│   ├── api/       # Exported interfaces (will be exported)
│   ├── handler/   # For RSocket handlers (if UI-facing)
│   └── internal/  # Private implementation (will be private)
└── pom.xml
```

## 2. Configure POM
Use the standard parent with relative path `../../pom.xml`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>io.github.sokybot</groupId>
        <artifactId>sokybot</artifactId>
        <version>1.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>sokybot-{name}</artifactId>
    <packaging>bundle</packaging>
    <name>Sokybot {Name}</name>
    <description>Description of the bundle</description>

    <dependencies>
        <!-- Add dependencies here -->
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.felix</groupId>
                <artifactId>maven-bundle-plugin</artifactId>
                <extensions>true</extensions>
                <configuration>
                    <instructions>
                        <Export-Package>org.sokybot.{name}.api.*</Export-Package>
                        <Private-Package>org.sokybot.{name}.internal.*</Private-Package>
                    </instructions>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## 3. Register in Category POM
Add to the `<modules>` section of the `{category}/pom.xml`.

```xml
<modules>
    <module>sokybot-{name}</module>
</modules>
```

## 4. Add to Karaf Feature (if needed)
Add the bundle to `infra/sokybot-features/src/main/feature/feature.xml`:

```xml
<bundle>mvn:io.github.sokybot/sokybot-{name}/${project.version}</bundle>
```

## 5. Build and Deploy
Use the `soky` CLI for building and deploying:

```bash
# Build only
soky build module sokybot-{name}

# Build and deploy (restarts backend with fresh cache)
soky deploy sokybot-{name}
```

## 6. Verify
Check the bundle is active:

```bash
soky backend shell
# Then in Karaf shell:
bundle:list | grep {name}
```
