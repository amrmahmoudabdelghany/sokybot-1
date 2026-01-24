---
trigger: always_on
glob: "**/*.java"
description: Security and Game Data (PK2) guidelines
---

# Security & Data Rules

## 1. Security Architecture (`sokybot-security`)
- **Encryption**: Silkroad Online uses Blowfish encryption.
- **Provider**: Use `IBlowfish` service from `sokybot-security`.
- **Integrity**: Use `ICRCSecurity` and `CountSecurity` for packet integrity.

**Rule**: Do not implement custom encryption logic. Use the provided services.

## 2. Game Data Access (`sokybot-pk2`)
- **Format**: PK2 is the archive format for game assets.
- **Access**: Use `Pk2File` to open and read PK2 archives.
- **Paths**: Game paths are case-insensitive but typically use backslashes `\`.

### Example
```java
// Open PK2 file
Pk2File pk2 = new Pk2File(new File("path/to/Data.pk2"));
try {
    // Read file entry
    JMXFile file = pk2.findFile("server_dep/silkroad/textdata/textdata_object.txt");
    // Process stream...
} finally {
    pk2.close();
}
```
