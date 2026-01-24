# Phase 4: Advanced Profiles & Build Optimization

This document summarizes the advanced profiles and build optimization improvements implemented in Phase 4.

## Overview

Phase 4 focuses on:
1. **Release Profile** - Production-ready builds with all quality checks
2. **Integration Test Profile** - Dedicated profile for running integration tests
3. **Reports Profile** - Comprehensive project reporting
4. **Build Optimization** - Incremental compilation and parallel builds
5. **Offline Profile** - Building without network access

## New Profiles

### 1. Release Profile (`-P release`)

Production release profile with maximum quality assurance:

```xml
<profile>
    <id>release</id>
    <properties>
        <skipTests>false</skipTests>
        <maven.test.skip>false</maven.test.skip>
        <spotbugs.skip>false</spotbugs.skip>
        <jacoco.skip>false</jacoco.skip>
        <checkstyle.skip>false</checkstyle.skip>
        <pmd.skip>false</pmd.skip>
        <owasp.skip>false</owasp.skip>
        <maven.compiler.debug>false</maven.compiler.debug>
        <maven.compiler.optimize>true</maven.compiler.optimize>
    </properties>
</profile>
```

**Features:**
- Runs all unit tests
- Enables all quality checks (SpotBugs, Checkstyle, PMD)
- Runs OWASP security scan
- Attaches source JARs for distribution
- Attaches Javadoc JARs for distribution
- Compiler optimizations (no debug info, optimized bytecode)

**Usage:**
```bash
./mvnw clean install -P release
```

### 2. Integration Test Profile (`-P integration-test`)

Dedicated profile for running integration tests:

```xml
<profile>
    <id>integration-test</id>
    <properties>
        <skipTests>false</skipTests>
        <skipITs>false</skipITs>
        <maven.test.skip>true</maven.test.skip>
    </properties>
</profile>
```

**Features:**
- Skips unit tests, runs only integration tests
- Uses Maven Failsafe Plugin
- Supports `*IT.java` and `*IntegrationTest.java` patterns
- Parallel test execution (2 threads)
- JaCoCo coverage enabled for IT

**Usage:**
```bash
# Run only integration tests
./mvnw verify -P integration-test

# Run both unit and integration tests
./mvnw verify -P ci -DskipITs=false
```

### 3. Reports Profile (`-P reports`)

Comprehensive project reporting:

```xml
<profile>
    <id>reports</id>
    <reporting>
        <plugins>
            <plugin>maven-project-info-reports-plugin</plugin>
            <plugin>maven-jxr-plugin</plugin>
            <plugin>maven-surefire-report-plugin</plugin>
            <plugin>jacoco-maven-plugin</plugin>
            <plugin>spotbugs-maven-plugin</plugin>
            <plugin>maven-checkstyle-plugin</plugin>
            <plugin>maven-pmd-plugin</plugin>
        </plugins>
    </reporting>
</profile>
```

**Features:**
- Project info and summary
- Dependency reports
- Dependency convergence analysis
- Module overview
- Cross-referenced source code (JXR)
- Test reports
- Code coverage (JaCoCo)
- Static analysis (SpotBugs, Checkstyle, PMD)

**Usage:**
```bash
./mvnw site -P reports
```

Reports are generated at `target/site/index.html`.

### 4. Offline Profile (`-P offline`)

For building without network access:

```xml
<profile>
    <id>offline</id>
    <properties>
        <skipTests>true</skipTests>
        <spotbugs.skip>true</spotbugs.skip>
        <jacoco.skip>true</jacoco.skip>
        <checkstyle.skip>true</checkstyle.skip>
        <pmd.skip>true</pmd.skip>
        <owasp.skip>true</owasp.skip>
    </properties>
</profile>
```

**Usage:**
```bash
./mvnw clean install -P offline -o
```

## Build Optimization

### Incremental Compilation

Enabled by default via:

```xml
<maven.compiler.useIncrementalCompilation>true</maven.compiler.useIncrementalCompilation>
```

This allows the compiler to only recompile changed files, significantly speeding up rebuilds.

### Parallel Builds

Configured in `.mvn/maven.config`:

```
-T4
```

Override on command line:
```bash
# Use 8 threads
./mvnw clean install -T8

# Use 1 thread per CPU core
./mvnw clean install -T1C
```

## Profile Summary

| Profile | Tests | Quality | OWASP | Optimized | Use Case |
|---------|-------|---------|-------|-----------|----------|
| `dev` | Skip | Skip | Skip | No | Daily development |
| `fast` | Skip | Skip | Skip | No | Quick iteration |
| `ci` | Run | Run | Skip | No | CI/CD pipelines |
| `release` | Run | Run | Run | Yes | Production releases |
| `security` | Skip | Skip | Run | No | Security scans |
| `integration-test` | ITs | Skip | Skip | No | Integration tests |
| `reports` | Skip | Run | Skip | No | Generate reports |
| `offline` | Skip | Skip | Skip | No | No network |

## New Plugin Versions

Added to root `pom.xml`:

```xml
<maven.failsafe.version>${maven.surefire.version}</maven.failsafe.version>
<maven.project.info.version>3.5.0</maven.project.info.version>
<maven.jxr.version>3.3.2</maven.jxr.version>
```

## Usage Examples

### Daily Development
```bash
./mvnw clean install                    # Uses dev profile by default
```

### Before Committing
```bash
./mvnw clean install -P ci              # Run all tests and quality checks
```

### Create a Release
```bash
./mvnw clean install -P release         # Full quality + security + optimized
```

### Run Integration Tests
```bash
./mvnw verify -P integration-test       # Only integration tests
```

### Generate Project Documentation
```bash
./mvnw site -P reports                  # Full project reports
```

### Quick Build for Testing
```bash
./mvnw clean install -P fast            # Skip everything for speed
```

### Security Audit
```bash
./mvnw verify -P security               # OWASP dependency check only
```

## Combining Profiles

Profiles can be combined for specific scenarios:

```bash
# Release with verbose output
./mvnw clean install -P release -X

# CI with integration tests
./mvnw verify -P ci,integration-test

# Fast build of specific module
./mvnw clean install -P fast -pl sokybot-engine -am
```

## Best Practices

1. **Use `dev` profile for daily work** - Fast feedback loop
2. **Run `ci` profile before pushing** - Catches issues early
3. **Use `release` profile for releases** - Maximum quality assurance
4. **Run `security` weekly** - Keep dependencies secure
5. **Generate `reports` periodically** - Track code quality trends
6. **Use parallel builds** - Faster compilation with `-T` flag
7. **Leverage incremental compilation** - Don't use `clean` unless necessary
