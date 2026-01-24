---
trigger: always_on
glob: "**/*.java"
description: Code style and conventions for Java code
---

# Code Conventions

## 1. Java Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Classes | PascalCase | `PacketHandler`, `GameContext` |
| Interfaces | PascalCase with `I` prefix | `IGameLoader`, `IRuteFinder` |
| Methods | camelCase | `handlePacket()`, `getContext()` |
| Variables | camelCase | `packetData`, `gameLoader` |
| Constants | UPPER_SNAKE_CASE | `MAX_RETRIES`, `DEFAULT_PORT` |
| Packages | lowercase | `org.sokybot.engine` |
| Type Parameters | Single uppercase | `T`, `E`, `K`, `V` |

## 2. Package Structure

```
org.sokybot.{module}/
├── api/           # Public interfaces
├── internal/      # Internal implementation (not exported)
├── domain/        # Domain models
├── service/       # Service implementations
└── util/          # Utilities
```

**Rule**: Only `api/` packages should be exported in OSGi bundles.

## 3. Class Organization

Order members as follows:
1. Static fields (constants first)
2. Instance fields
3. Constructors
4. Public methods
5. Protected methods
6. Private methods
7. Inner classes

## 4. Javadoc Requirements

**Required for**:
- All public classes
- All public methods
- Complex private methods

**Template**:
```java
/**
 * Brief description.
 *
 * @param paramName description
 * @return description
 * @throws ExceptionType when condition
 */
```

## 5. Lombok Usage

This project uses Lombok. Preferred annotations:

| Annotation | Use For |
|------------|---------|
| `@Getter/@Setter` | Simple properties |
| `@Data` | DTOs and value objects |
| `@Builder` | Complex object construction |
| `@Slf4j` | Logging (preferred over manual logger) |
| `@RequiredArgsConstructor` | Dependency injection |

**Avoid**: `@AllArgsConstructor` in OSGi services (breaks DI).

## 6. Logging

Use SLF4J with Lombok's `@Slf4j`:

```java
@Slf4j
public class MyService {
    public void process() {
        log.debug("Processing started");
        log.info("Processed {} items", count);
        log.error("Failed to process", exception);
    }
}
```

**Levels**:
- `ERROR` - Failures requiring attention
- `WARN` - Potential issues
- `INFO` - Significant events
- `DEBUG` - Development/troubleshooting
- `TRACE` - Detailed debugging

## 7. Exception Handling

```java
// DO: Specific exceptions with context
throw new GameLoadException("Failed to load map: " + mapId, cause);

// DON'T: Generic exceptions
throw new Exception("Error");

// DO: Log and wrap
catch (IOException e) {
    log.error("Failed to read file: {}", path, e);
    throw new DataLoadException("Cannot read " + path, e);
}

// DON'T: Swallow exceptions
catch (Exception e) {
    // silently ignored - BAD!
}
```

## 8. Null Handling

- Prefer `Optional<T>` for return types that may be absent
- Use `@NonNull` / `@Nullable` annotations
- Check parameters at method entry:

```java
public void process(@NonNull String input) {
    Objects.requireNonNull(input, "input must not be null");
    // ...
}
```

## 9. Formatting Rules

| Rule | Value |
|------|-------|
| Indentation | 4 spaces (no tabs) |
| Line length | 120 characters max |
| Braces | Same line (K&R style) |
| Blank lines | 1 between methods, 2 between sections |

## 10. Import Organization

Order:
1. `java.*`
2. `javax.*`
3. Third-party libraries
4. `org.sokybot.*`

**Rules**:
- No wildcard imports (`import java.util.*`)
- No unused imports
- Static imports at the end

## 11. Common Utilities Usage

Before implementing new utilities, check `sokybot-commons`:

| Utility Class | Purpose |
|---------------|---------|
| `Hexdump` | Debug logging of byte arrays |
| `Bytes` | Byte array manipulation |
| `Helper` | General string/number parsing |
| `SokybotIOUtils` | Stream handling |
| `Converter` | Type conversion |

**Rule**: Do not duplicate minimal utility logic if it exists in `sokybot-commons`.

