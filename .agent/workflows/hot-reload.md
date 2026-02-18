---
description: Fast hot-reloading of Java modules without container restarts
---

This workflow allows you to apply changes to Java modules and see them reflected in the running backend almost instantly.

1. **Modify Code**: Make your changes in the Java source files.
2. **Build on Host**: Build the specific module on your host machine to avoid redirection overhead.
   ```bash
   ./mvnw install -pl <module-name> -DskipTests -P fast
   ```
3. **Hot-Reload**: The automated watcher inside Docker will detect the new JAR and update the bundle in Karaf.
   - Alternatively, trigger it manually:
   ```bash
   soky refresh <module-name>
   ```
4. **Verify**: Check the UI or Karaf logs to confirm the change is active.
   ```bash
   soky backend logs
   ```

// turbo-all
> [!TIP]
> Use `soky test <module-name>` to verify logic before deploying.
