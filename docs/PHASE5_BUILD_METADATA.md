# Phase 5: Build Reproducibility & Metadata

This document summarizes the build reproducibility and metadata improvements implemented in Phase 5.

## Overview

Phase 5 focuses on:
1. **Git Commit ID Plugin** - Embeds Git information in builds
2. **Reproducible Builds** - Fixed output timestamps for identical builds
3. **Version Rules** - Smart filtering of dependency updates
4. **Character Encoding** - Standardized UTF-8 encoding

## Changes Made

### 1. Git Commit ID Plugin

Added `git-commit-id-maven-plugin` to embed Git information in builds:

```xml
<git-commit-id.version>7.0.0</git-commit-id.version>

<plugin>
    <groupId>io.github.git-commit-id</groupId>
    <artifactId>git-commit-id-maven-plugin</artifactId>
    <configuration>
        <generateGitPropertiesFile>true</generateGitPropertiesFile>
        <generateGitPropertiesFilename>${project.build.outputDirectory}/git.properties</generateGitPropertiesFilename>
        <includeOnlyProperties>
            <includeOnlyProperty>^git.build.(time|version)$</includeOnlyProperty>
            <includeOnlyProperty>^git.commit.id.(abbrev|full)$</includeOnlyProperty>
            <includeOnlyProperty>^git.commit.message.short$</includeOnlyProperty>
            <includeOnlyProperty>^git.branch$</includeOnlyProperty>
            <includeOnlyProperty>^git.dirty$</includeOnlyProperty>
        </includeOnlyProperties>
        <failOnNoGitDirectory>false</failOnNoGitDirectory>
        <skip>${git.skip}</skip>
    </configuration>
</plugin>
```

**Features:**
- Generates `git.properties` in the classpath
- Includes commit ID, branch, build time, dirty flag
- Skipped in `dev` and `fast` profiles for speed
- Enabled in `ci` and `release` profiles

**Usage:**
```bash
# Build with git info
./mvnw clean package -P ci

# View git properties
cat target/classes/git.properties
```

**Example git.properties:**
```properties
git.branch=main
git.build.time=2024-01-15T10\:30\:00+0000
git.commit.id.abbrev=a1b2c3d
git.commit.id.full=a1b2c3d4e5f6789...
git.commit.message.short=Add new feature
git.dirty=false
```

### 2. Reproducible Builds

Added output timestamp for reproducible builds:

```xml
<project.build.outputTimestamp>2024-01-01T00:00:00Z</project.build.outputTimestamp>
```

**Benefits:**
- Identical artifacts from same source code
- Verifiable builds
- Better caching in CI/CD

**Note:** Update this timestamp when making a release to reflect the release date.

### 3. Version Update Rules

Created `versions-rules.xml` for smart dependency update filtering:

```xml
<ruleset>
    <rules>
        <!-- Ignore pre-release versions globally -->
        <rule groupId="*" artifactId="*">
            <ignoreVersions>
                <ignoreVersion type="regex">.*[-.]alpha.*</ignoreVersion>
                <ignoreVersion type="regex">.*[-.]beta.*</ignoreVersion>
                <ignoreVersion type="regex">.*[-.]RC.*</ignoreVersion>
                <ignoreVersion type="regex">.*-M[0-9]+.*</ignoreVersion>
            </ignoreVersions>
        </rule>
        
        <!-- Stay on SLF4J 1.7.x -->
        <rule groupId="org.slf4j" artifactId="*">
            <ignoreVersions>
                <ignoreVersion type="regex">2\..*</ignoreVersion>
            </ignoreVersions>
        </rule>
        
        <!-- Stay on Hibernate 5.x -->
        <rule groupId="org.hibernate" artifactId="*">
            <ignoreVersions>
                <ignoreVersion type="regex">6\..*</ignoreVersion>
            </ignoreVersions>
        </rule>
        
        <!-- Stay on Spring 5.x -->
        <rule groupId="org.springframework" artifactId="*">
            <ignoreVersions>
                <ignoreVersion type="regex">6\..*</ignoreVersion>
            </ignoreVersions>
        </rule>
    </rules>
</ruleset>
```

**Filtered versions:**
- Alpha, beta, milestone, RC versions
- SLF4J 2.x (incompatible API)
- Hibernate 6.x (requires Java 17)
- Spring 6.x (requires Java 17)
- Logback 1.5.x (requires SLF4J 2.x)

**Usage:**
```bash
# Check for updates (rules applied automatically)
./mvnw versions:display-dependency-updates

# Check plugin updates
./mvnw versions:display-plugin-updates
```

### 4. Character Encoding

Standardized UTF-8 encoding:

```xml
<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
```

## Profile Updates

| Profile | git.skip | Description |
|---------|----------|-------------|
| `dev` | true | Fast development builds |
| `fast` | true | Quickest builds |
| `ci` | false | CI builds include git info |
| `release` | false | Releases include git info |

## New Files

### versions-rules.xml

Located at project root, automatically used by versions-maven-plugin.

**Filters out:**
- Pre-release versions (alpha, beta, RC, milestone)
- Incompatible major versions for key libraries
- Preview and incubating releases

## Usage Examples

### Check Build Info
```bash
# Build with CI profile (includes git info)
./mvnw clean package -P ci

# Check the generated git.properties
cat target/classes/git.properties
```

### Check for Updates
```bash
# Display dependency updates (filtered by rules)
./mvnw versions:display-dependency-updates

# Display plugin updates
./mvnw versions:display-plugin-updates

# Display property updates
./mvnw versions:display-property-updates
```

### Update Dependencies
```bash
# Update specific dependency
./mvnw versions:use-latest-versions -Dincludes=org.apache.commons:commons-lang3

# Update all (respects rules)
./mvnw versions:use-latest-versions
```

## Best Practices

1. **Update output timestamp for releases** - Change `project.build.outputTimestamp` when releasing
2. **Run version checks periodically** - Use `versions:display-dependency-updates` monthly
3. **Review rules when upgrading** - Update `versions-rules.xml` when major upgrades are planned
4. **Use CI profile for deployable builds** - Ensures git info is embedded
5. **Keep encoding consistent** - All files should be UTF-8

## Accessing Git Info at Runtime

Example Java code to read git properties:

```java
import java.io.InputStream;
import java.util.Properties;

public class BuildInfo {
    private static final Properties gitProperties = new Properties();
    
    static {
        try (InputStream is = BuildInfo.class.getResourceAsStream("/git.properties")) {
            if (is != null) {
                gitProperties.load(is);
            }
        } catch (Exception e) {
            // Git properties not available
        }
    }
    
    public static String getCommitId() {
        return gitProperties.getProperty("git.commit.id.abbrev", "unknown");
    }
    
    public static String getBranch() {
        return gitProperties.getProperty("git.branch", "unknown");
    }
    
    public static String getBuildTime() {
        return gitProperties.getProperty("git.build.time", "unknown");
    }
    
    public static boolean isDirty() {
        return Boolean.parseBoolean(gitProperties.getProperty("git.dirty", "false"));
    }
}
```
