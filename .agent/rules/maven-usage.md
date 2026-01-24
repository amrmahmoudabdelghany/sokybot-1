---
trigger: always_on
glob: "**/pom.xml"
description: Maven build system usage and profiles
---

# Maven Usage Guide

## 1. Maven Wrapper

**Always use the wrapper** for consistent builds (Maven 3.9.6+ enforced).

### Essential Plugins (Enforced)
- **Maven Enforcer**: Requires Java 21+, Maven 3.6.0+, and dependency convergence.
- **Versions Plugin**: Used to manage and update dependencies/plugins.


```bash
./mvnw <command>    # Linux/macOS
mvnw.cmd <command>  # Windows
```

## 2. Build Profiles

| Profile | Command | Use Case |
|---------|---------|----------|
| `dev` (default) | `./mvnw install` | Daily development (skips tests) |
| `fast` | `./mvnw install -P fast` | Quickest builds (skips everything) |
| `ci` | `./mvnw install -P ci` | CI/CD (all tests + quality checks) |
| `release` | `./mvnw install -P release` | Production releases |
| `security` | `./mvnw verify -P security` | OWASP security scan |
| `desktop` | `./mvnw install -P desktop` | Electron distribution |
| `integration-test` | `./mvnw verify -P integration-test` | Run integration tests |
| `reports` | `./mvnw site -P reports` | Generate all reports |

## 3. Common Commands

### Building
```bash
# Fast development build
./mvnw clean install -P fast

# Build specific module
./mvnw clean install -pl sokybot-engine -am

# Skip tests
./mvnw install -DskipTests

# Validate config only (fast)
./mvnw validate -P fast
```

### Quality Checks
```bash
# Run Checkstyle
./mvnw checkstyle:check

# Run SpotBugs
./mvnw spotbugs:check

# Run PMD
./mvnw pmd:check

# Run all quality checks
./mvnw verify -P ci
```

### Dependency Management
```bash
# Check for updates
./mvnw versions:display-dependency-updates

# Check plugin updates
./mvnw versions:display-plugin-updates

# Analyze dependencies
./mvnw dependency:analyze
```

## 4. Profile Behavior Matrix

| Profile | Tests | SpotBugs | Checkstyle | PMD | OWASP | Git Info |
|---------|-------|----------|------------|-----|-------|----------|
| `dev` | Skip | Skip | Skip | Skip | Skip | Skip |
| `fast` | Skip | Skip | Skip | Skip | Skip | Skip |
| `ci` | Run | Run | Run | Run | Skip | Run |
| `release` | Run | Run | Run | Run | Run | Run |
| `security` | Skip | Skip | Skip | Skip | **Run** | Skip |

## 5. Module-Specific Builds

Build only what you need:

```bash
# Single module (no dependencies)
./mvnw install -pl sokybot-commons

# Module + dependencies
./mvnw install -pl sokybot-engine -am

# Module + dependents
./mvnw install -pl sokybot-commons -amd

# Multiple modules
./mvnw install -pl sokybot-commons,sokybot-security -am
```

## 6. Distribution Builds

```bash
# Development Karaf
./mvnw install -P dev -pl sokybot-dist -am

# Desktop distribution
./mvnw install -P desktop -pl sokybot-dist -am
```

## 7. Troubleshooting

```bash
# Force dependency updates
./mvnw install -U

# Debug output
./mvnw install -X

# Skip specific checks
./mvnw install -Dcheckstyle.skip=true -Dspotbugs.skip=true

# Offline mode
./mvnw install -o
```

## 8. POM Editing Rules

When editing `pom.xml` files:

1. **Versions**: Define in root `<properties>`, reference with `${property.name}`
2. **Dependencies**: Use `<dependencyManagement>` in root for versions
3. **Plugins**: Use `<pluginManagement>` in root for configuration
4. **Scopes**: 
   - `provided` for OSGi runtime dependencies
   - `compile` for embedded dependencies
   - `test` for test dependencies

## 9. OSGi Bundle Configuration

For OSGi bundles, configure in `maven-bundle-plugin`:

```xml
<Bundle-SymbolicName>${project.groupId}.${project.artifactId}</Bundle-SymbolicName>
<Bundle-Name>${project.name}</Bundle-Name>
<Bundle-Version>${project.version}</Bundle-Version>
<Export-Package>org.sokybot.module.api.*</Export-Package>
<Private-Package>org.sokybot.module.internal.*</Private-Package>
<Import-Package>*;resolution:=optional</Import-Package>
```
