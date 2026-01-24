---
trigger: always_on
glob:
description: Build and verification constraints for large codebase
---

# Build & Verification Constraints

## 1. Default Rule: No Full Builds

**AVOID** running full build commands that take significant time:
- `mvn install` (full project)
- `mvn compile` (full project)
- `npm run build`
- `docker build`

**REASONING**: This is a large multi-module codebase. Full builds disrupt workflow.

## 2. Allowed Quick Validations

These fast commands ARE permitted when necessary:

| Command | Purpose | When to Use |
|---------|---------|-------------|
| `./mvnw validate -P fast` | Syntax check | After pom.xml changes |
| `./mvnw compile -pl module -P fast` | Single module | After significant changes |
| `./mvnw help:effective-pom` | Check POM resolution | Debugging dependencies |

## 3. When Full Builds ARE Appropriate

You MAY run full builds when:
- User explicitly requests verification
- Making changes to build configuration (pom.xml, plugins)
- Changes affect multiple interdependent modules
- Creating distribution packages

## 4. Verification Strategy

Instead of building to verify:

1. **Trust the IDE** - Rely on linter errors shown
2. **Check imports** - Ensure all imports are valid
3. **Review dependencies** - Verify module dependencies exist
4. **Use ReadLints** - Check for reported errors

## 5. Output Protocol

When making changes:
1. Make the changes
2. Briefly note what was changed
3. Suggest verification command user can run
4. Do NOT run verification yourself unless in exceptions above