# Sokybot Developer Guide

This guide provides common Maven commands and development workflows for the Sokybot project.

## Prerequisites

- **Java 11** or higher
- **Maven 3.6+** (or use the included Maven Wrapper - see below)

## Maven Wrapper

The project includes a Maven Wrapper (`mvnw` / `mvnw.cmd`), which ensures all developers use the same Maven version.

### Using Maven Wrapper

**Linux/Mac:**
```bash
./mvnw clean install
./mvnw compile
```

**Windows:**
```cmd
mvnw.cmd clean install
mvnw.cmd compile
```

If you prefer using your system Maven installation, you can use `mvn` instead of `./mvnw`.

## RSocket webview protocol

For UI ↔ backend framing (streams, fire-and-forget, request-channel, metadata routing, and stream backpressure notes), see [RSOCKET_WEBVIEW.md](./RSOCKET_WEBVIEW.md).

## Common Maven Commands

### Building the Project

```bash
# Clean and compile all modules
./mvnw clean compile

# Clean, compile, and run tests
./mvnw clean test

# Clean, compile, test, and package all modules
./mvnw clean install

# Skip tests during build (faster iteration)
./mvnw clean install -DskipTests

# Build only specific modules
./mvnw clean install -pl sokybot-engine,sokybot-commons -am
```

### Development Profile (Default)

The `dev` profile is active by default and:
- **Skips test execution** (but still compiles tests)
- Enables faster builds for development

```bash
# Build with dev profile (default)
./mvnw clean install

# Explicitly use dev profile
./mvnw clean install -P dev
```

### Fast Build Profile

For quick iteration when you don't need tests or quality checks:

```bash
./mvnw clean install -P fast
```

### CI Profile

For continuous integration builds that run all tests:

```bash
./mvnw clean install -P ci
```

### Production Profile

For production builds with specific module selection:

```bash
./mvnw clean install -P prod
```

### Release Profile

For creating production releases with all quality checks, optimizations, and documentation:

```bash
./mvnw clean install -P release
```

This profile:
- Enables all quality checks (SpotBugs, Checkstyle, PMD, OWASP)
- Runs all tests
- Attaches source JARs
- Attaches Javadoc JARs
- Applies compiler optimizations (no debug info, optimized bytecode)

### Integration Test Profile

For running integration tests:

```bash
./mvnw verify -P integration-test
```

This profile runs tests matching `*IT.java` or `*IntegrationTest.java` patterns.

### Reports Profile

For generating comprehensive project reports:

```bash
./mvnw site -P reports
```

Generates reports at `target/site/` including:
- Project info and dependencies
- Code coverage (JaCoCo)
- Static analysis (SpotBugs, Checkstyle, PMD)
- Test reports
- Cross-referenced source code

### Offline Profile

For building without network access:

```bash
./mvnw clean install -P offline -o
```

### Security Profile

For running security vulnerability scans:

```bash
./mvnw verify -P security
```

### Desktop Profile

For creating desktop distributions (Electron deployment):

```bash
./mvnw clean install -P desktop -pl sokybot-dist -am
```

This creates a self-contained bundle in `sokybot-dist/target/` with:
- Karaf runtime (console disabled)
- Launch scripts for Windows/Mac/Linux
- JRE placeholder (bundle separately)

## Desktop Packaging (Electron Deployment)

### Build Desktop Distribution

```bash
# Build desktop bundle
./mvnw clean install -P desktop -pl sokybot-dist -am

# Find the output
ls sokybot-dist/target/Sokybot-*-desktop/
```

### Output Structure

```
Sokybot-1.0-SNAPSHOT/
├── sokybot.bat        # Windows launcher
├── sokybot.sh         # Linux/macOS launcher
├── sokybot.ps1        # PowerShell launcher
├── jre/               # Bundle JRE here
└── runtime/           # Karaf + app bundles
```

### Bundling a JRE

For a self-contained distribution:

1. Download JRE 11+ from https://adoptium.net/
2. Extract to the `jre/` folder
3. The launcher auto-detects bundled JRE

### Environment Variables

| Variable | Description |
|----------|-------------|
| `SOKYBOT_OPTS` | Additional JVM options (e.g., `-Xmx2g`) |
| `SOKYBOT_DEBUG` | Set to `true` for verbose output |
| `JAVA_HOME` | Path to JRE (if not bundled) |

## Working with Modules

### Build a Single Module

```bash
# Build only sokybot-engine and its dependencies
./mvnw clean install -pl sokybot-engine -am

# Build only sokybot-engine (skip dependencies)
./mvnw clean install -pl sokybot-engine
```

### Build Distribution

```bash
# Build the Karaf distribution
./mvnw clean install -pl sokybot-features,sokybot-dist -am -P dev
```

## Testing

### Run All Tests

```bash
# Run all tests
./mvnw test

# Run tests with CI profile (ensures tests are not skipped)
./mvnw test -P ci
```

### Run Tests for a Specific Module

```bash
# Run tests only for sokybot-engine
./mvnw test -pl sokybot-engine
```

### Skip Tests

```bash
# Skip test execution (but compile them)
./mvnw clean install -DskipTests

# Skip test compilation entirely (faster, but not recommended)
./mvnw clean install -Dmaven.test.skip=true
```

### Integration Tests

Integration tests should be named with `*IT.java` or `*IntegrationTest.java` suffix.

```bash
# Run integration tests
./mvnw verify -P integration-test

# Run both unit and integration tests
./mvnw verify -P ci -DskipITs=false
```

## Dependency Management

### Check for Dependency Updates

```bash
# Display all dependencies that have newer versions available
./mvnw versions:display-dependency-updates

# Display all plugins that have newer versions available
./mvnw versions:display-plugin-updates

# Display all property updates
./mvnw versions:display-property-updates
```

The project includes `versions-rules.xml` which automatically filters out:
- Alpha, beta, milestone, and RC versions
- Incompatible major versions (e.g., SLF4J 2.x, Spring 6.x)

### Update Dependency Versions

```bash
# Update a specific dependency to latest version
./mvnw versions:use-latest-versions -Dincludes=org.apache.commons:commons-lang3

# Update all dependencies to latest versions (use with caution!)
./mvnw versions:use-latest-versions
```

## Build Information

### Git Commit ID

The project automatically embeds Git information in builds (CI and release profiles).

```bash
# View git info in built artifact
cat target/classes/git.properties
```

The `git.properties` file includes:
- `git.commit.id.full` - Full commit hash
- `git.commit.id.abbrev` - Abbreviated commit hash
- `git.branch` - Current branch
- `git.build.time` - Build timestamp
- `git.dirty` - Whether there are uncommitted changes

### Reproducible Builds

The project is configured for reproducible builds with a fixed output timestamp.
This ensures identical artifacts when built from the same source.

## Code Quality

### Maven Enforcer Plugin

The project includes Maven Enforcer Plugin to ensure:
- Minimum Maven version (3.6.0)
- Java version (11)
- Dependency convergence (no version conflicts)

```bash
# Run enforcer checks
./mvnw enforcer:enforce
```

### Static Analysis Tools

```bash
# Run SpotBugs (bug detection)
./mvnw spotbugs:spotbugs

# Run Checkstyle (code style)
./mvnw checkstyle:checkstyle

# Run PMD (code analysis)
./mvnw pmd:pmd

# Run all quality checks with CI profile
./mvnw verify -P ci
```

## Security Scanning

### OWASP Dependency Check

The project includes OWASP Dependency Check for scanning dependencies for known vulnerabilities.

```bash
# Run full security scan
./mvnw verify -P security

# Run only OWASP dependency check
./mvnw dependency-check:aggregate

# Run dependency analysis
./mvnw dependency:analyze
```

### Security Profile

The `security` profile is optimized for security scanning:
- Skips tests and other quality checks
- Only runs OWASP dependency check
- Generates reports in HTML, XML, and JSON formats

```bash
./mvnw verify -P security
```

Reports are generated at:
- `target/dependency-check-report.html` - Human-readable report
- `target/dependency-check-report.xml` - Machine-readable XML
- `target/dependency-check-report.json` - JSON format

### Vulnerability Thresholds

The build will fail if any vulnerability with CVSS score >= 7 (High/Critical) is found.

| CVSS Score | Severity | Build Action |
|------------|----------|--------------|
| 0.0 - 6.9  | Low-Medium | Warning only |
| 7.0 - 10.0 | High-Critical | **Build fails** |

## IDE Integration

### Generate IDE Files

```bash
# Generate Eclipse project files
./mvnw eclipse:eclipse

# Generate IntelliJ IDEA project files
./mvnw idea:idea
```

### Import into IntelliJ IDEA

1. Open IntelliJ IDEA
2. File → Open → Select the `sokybot` directory
3. IntelliJ will automatically detect the Maven project
4. Wait for indexing to complete

### Import into Eclipse

1. File → Import → Maven → Existing Maven Projects
2. Select the `sokybot` directory
3. Click Finish

## Running the Application

### Build and Run Karaf Distribution

```bash
# Build the distribution
./mvnw clean install -pl sokybot-features,sokybot-dist -am -P dev

# Run Karaf (if run-karaf.sh exists)
cd sokybot
./run-karaf.sh

# Or run directly
cd sokybot-dist/target/assembly
./bin/karaf
```

## Troubleshooting

### Clean Build

If you encounter strange build errors, try a clean build:

```bash
# Clean all modules
./mvnw clean

# Clean and rebuild
./mvnw clean install
```

### Update Snapshots

If you're working with SNAPSHOT dependencies:

```bash
# Force update of SNAPSHOT dependencies
./mvnw clean install -U
```

### Offline Mode

To work offline (uses cached dependencies):

```bash
./mvnw clean install -o
```

### Debug Build Issues

```bash
# Run with debug output
./mvnw clean install -X

# Run with error stack traces
./mvnw clean install -e
```

## Project Structure

```
sokybot/
├── pom.xml                    # Root POM
├── mvnw / mvnw.cmd           # Maven Wrapper
├── .mvn/                      # Maven configuration
│   └── maven.config          # Default Maven options
├── sokybot-commons/          # Common utilities
├── sokybot-engine/            # Core engine
├── sokybot-dist/              # Karaf distribution
└── ...                        # Other modules
```

## Best Practices

1. **Always use Maven Wrapper** (`./mvnw`) to ensure consistent builds across the team
2. **Use `-P dev` profile** for daily development (skips tests for faster builds)
3. **Use `-P ci` profile** before committing (runs all tests)
4. **Run `./mvnw clean install`** after pulling changes from Git
5. **Check for dependency updates** periodically using `versions:display-dependency-updates`

## Useful Maven Properties

The project defines several properties in the root `pom.xml`:

- `jna.version` - JNA library version
- `junit-jupiter.version` - JUnit 5 version
- `mockito.version` - Mockito version
- `commons-lang3.version` - Apache Commons Lang3 version
- `logback.version` - Logback version
- `slf4j.version` - SLF4J version
- `lombok.version` - Lombok version

All dependency versions are managed in the root POM's `<dependencyManagement>` section.

## Profile Summary

| Profile | Tests | Quality Checks | OWASP | Use Case |
|---------|-------|----------------|-------|----------|
| `dev` (default) | Skipped | Skipped | Skipped | Daily development |
| `fast` | Skipped | Skipped | Skipped | Quick iteration |
| `ci` | Run | Run | Skipped | CI/CD pipelines |
| `release` | Run | Run | Run | Production releases |
| `security` | Skipped | Skipped | Run | Security scans |
| `integration-test` | ITs only | Skipped | Skipped | Integration testing |
| `reports` | Skipped | Run | Skipped | Generate reports |
| `offline` | Skipped | Skipped | Skipped | No network access |
| `desktop` | Skipped | Skipped | Skipped | Electron distribution |
| `windows`/`mac`/`linux` | - | - | - | Auto-detected platform |

### Combining Profiles

You can combine profiles for specific scenarios:

```bash
# Full release with security check
./mvnw clean install -P release

# CI with integration tests
./mvnw verify -P ci,integration-test

# Quick build with specific module
./mvnw clean install -P fast -pl sokybot-engine -am
```

## Build Optimization Tips

### Parallel Builds

The project is configured for parallel builds by default (4 threads). Override with:

```bash
# Use 8 threads
./mvnw clean install -T8

# Use 1 thread per CPU core
./mvnw clean install -T1C
```

### Incremental Compilation

Incremental compilation is enabled by default. For a full recompile:

```bash
./mvnw clean compile
```

### Skip Specific Checks

```bash
# Skip SpotBugs only
./mvnw verify -Dspotbugs.skip=true

# Skip Checkstyle only
./mvnw verify -Dcheckstyle.skip=true

# Skip JaCoCo coverage
./mvnw verify -Djacoco.skip=true
```

## Additional Resources

- [Maven Documentation](https://maven.apache.org/guides/)
- [Maven Wrapper Documentation](https://maven.apache.org/wrapper/)
- [OSGi Bundle Development](https://www.osgi.org/developer/specifications/)
