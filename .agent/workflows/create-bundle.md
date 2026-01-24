---
description: How to create a new OSGi Bundle
---

# How to Create a New OSGi Bundle

## 1. Create Directory Structure
Choose a category (e.g., `core`, `game`) and create the module inside it:

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
<parent>
    <groupId>io.github.sokybot</groupId>
    <artifactId>sokybot</artifactId>
    <version>1.0-SNAPSHOT</version>
    <relativePath>../../pom.xml</relativePath>
</parent>
...
```

## 3. Register in Category POM
Add to the `<modules>` section of the `{category}/pom.xml`.

## 4. Build
Run `./mvnw install -pl sokybot-{name}` to verify.
