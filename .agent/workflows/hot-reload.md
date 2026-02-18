---
description: Fast hot-reloading of Java modules without container restarts
---

## Option 1: Groovy Scripts (Near-Instant)
Changes to bot logic in `scripts/actuators/` or UI pages in `scripts/pages/` are detected by the internal `WatchService`.

1. **Modify Script**: Edit the `.groovy` or `.json` file.
2. **Save**: The change is applied instantly without any Maven build or container refresh.
3. **Verify**: Check `soky backend logs` for re-compilation status.

---

## Option 2: Java Modules (requires Build)
Use this for base infrastructure or legacy Java actuators.

1. **Modify Code**: Make your changes in the Java source files.
2. **Build on Host**: Build the specific module.
   ```bash
   ./mvnw install -pl <module-name> -DskipTests -P fast
   ```
3. **Hot-Reload**: The automated watcher inside Docker will detect the new JAR and update the bundle in Karaf.
   - Alternatively: `soky refresh <module-name>`
4. **Verify**: `soky backend logs`.

// turbo-all
> [!TIP]
> Use `soky test <module-name>` to verify logic before deploying.
