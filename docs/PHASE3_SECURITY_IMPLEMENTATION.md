# Phase 3: Security & Performance Implementation

This document summarizes the security and performance improvements implemented in Phase 3.

## Overview

Phase 3 focuses on:
1. **OWASP Dependency Check** - Security vulnerability scanning for dependencies
2. **Maven Dependency Plugin** - Dependency analysis and management
3. **Profile Updates** - Dedicated security profile for running security scans

## Changes Made

### 1. OWASP Dependency Check Plugin

Added to root `pom.xml`:

```xml
<owasp.version>9.0.9</owasp.version>

<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>${owasp.version}</version>
</plugin>

<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <skipProvidedScope>true</skipProvidedScope>
        <skipRuntimeScope>false</skipRuntimeScope>
        <skipTestScope>true</skipTestScope>
        <formats>
            <format>HTML</format>
            <format>XML</format>
            <format>JSON</format>
        </formats>
        <skip>${owasp.skip}</skip>
    </configuration>
</plugin>
```

**Features:**
- Scans all project dependencies for known vulnerabilities
- Fails build on CVSS score >= 7 (High/Critical vulnerabilities)
- Generates reports in HTML, XML, and JSON formats
- Skips test and provided scope dependencies
- Can be skipped with `-Dowasp.skip=true`

### 2. Maven Dependency Plugin

Added dependency analysis capabilities:

```xml
<maven.dependency.version>3.6.1</maven.dependency.version>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <configuration>
        <ignoredUnusedDeclaredDependencies>
            <ignoredUnusedDeclaredDependency>org.projectlombok:lombok</ignoredUnusedDeclaredDependency>
            <ignoredUnusedDeclaredDependency>org.osgi:*</ignoredUnusedDeclaredDependency>
        </ignoredUnusedDeclaredDependencies>
    </configuration>
</plugin>
```

**Features:**
- Analyzes dependencies for unused/undeclared dependencies
- Ignores known compile-time-only dependencies (Lombok, OSGi annotations)

### 3. Profile Updates

#### Updated Profiles

**`dev` profile** - Added OWASP skip:
```xml
<owasp.skip>true</owasp.skip>
```

**`fast` profile** - Added all quality skips:
```xml
<spotbugs.skip>true</spotbugs.skip>
<jacoco.skip>true</jacoco.skip>
<checkstyle.skip>true</checkstyle.skip>
<pmd.skip>true</pmd.skip>
<owasp.skip>true</owasp.skip>
```

**`ci` profile** - OWASP skipped by default (runs in separate security job):
```xml
<owasp.skip>true</owasp.skip>
```

#### New `security` Profile

Dedicated profile for running security scans:

```xml
<profile>
    <id>security</id>
    <properties>
        <skipTests>true</skipTests>
        <spotbugs.skip>true</spotbugs.skip>
        <jacoco.skip>true</jacoco.skip>
        <checkstyle.skip>true</checkstyle.skip>
        <pmd.skip>true</pmd.skip>
        <owasp.skip>false</owasp.skip>
    </properties>
    <build>
        <plugins>
            <plugin>
                <groupId>org.owasp</groupId>
                <artifactId>dependency-check-maven</artifactId>
                <executions>
                    <execution>
                        <id>security-check</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>aggregate</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</profile>
```

### 4. GitHub Actions Updates

Added security scanning job to `.github/workflows/ci.yml`:

```yaml
security:
  runs-on: ubuntu-latest
  if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/master' || github.event_name == 'schedule'
  
  steps:
    - name: Cache NVD database
      uses: actions/cache@v4
      with:
        path: ~/.m2/repository/org/owasp/dependency-check-data
        key: ${{ runner.os }}-owasp-${{ github.run_id }}
    
    - name: Run OWASP Dependency Check
      run: ./mvnw dependency-check:aggregate -P security
    
    - name: Upload OWASP report
      uses: actions/upload-artifact@v4
      with:
        name: owasp-dependency-check
        path: target/dependency-check-report.*
```

**Features:**
- Runs on main/master branches only (not PRs)
- Weekly scheduled runs (Sundays at 2 AM UTC)
- Caches NVD database for faster subsequent runs
- Uploads security reports as artifacts

## Usage

### Run Security Scan Locally

```bash
# Full security scan
./mvnw verify -P security

# Just OWASP dependency check
./mvnw dependency-check:aggregate

# Check for updates to dependencies
./mvnw versions:display-dependency-updates

# Analyze dependencies
./mvnw dependency:analyze
```

### View Reports

After running the security scan, reports are available at:
- `target/dependency-check-report.html` - Human-readable HTML report
- `target/dependency-check-report.xml` - Machine-readable XML report
- `target/dependency-check-report.json` - JSON format for integrations

### Suppressing False Positives

Create a `dependency-check-suppressions.xml` file to suppress known false positives:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
    <suppress>
        <notes>Example suppression</notes>
        <packageUrl regex="true">^pkg:maven/com\.example/artifact@.*$</packageUrl>
        <cve>CVE-XXXX-XXXXX</cve>
    </suppress>
</suppressions>
```

Then reference it in the plugin configuration:
```xml
<suppressionFile>dependency-check-suppressions.xml</suppressionFile>
```

## CVSS Severity Levels

The OWASP plugin uses CVSS scores to determine severity:

| CVSS Score | Severity | Action |
|------------|----------|--------|
| 0.0        | None     | Informational |
| 0.1 - 3.9  | Low      | Monitor |
| 4.0 - 6.9  | Medium   | Plan remediation |
| 7.0 - 8.9  | High     | **Build fails** |
| 9.0 - 10.0 | Critical | **Build fails** |

The current configuration fails builds on CVSS >= 7 (High and Critical vulnerabilities).

## Best Practices

1. **Regular Scans**: Run security scans at least weekly in CI/CD
2. **Update Dependencies**: Keep dependencies updated to avoid known vulnerabilities
3. **Review Reports**: Regularly review OWASP reports for new vulnerabilities
4. **Suppress Wisely**: Only suppress false positives with proper documentation
5. **Use versions:display-dependency-updates**: Check for newer versions of dependencies

## Performance Considerations

OWASP Dependency Check can be slow due to:
- NVD database downloads (first run takes ~10 minutes)
- Scanning all dependencies

Optimizations implemented:
- NVD database caching in CI
- Separate security job (doesn't block main build)
- Skip in development profiles
- Weekly scheduled runs instead of every push
