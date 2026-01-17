# PK2 Testing Framework

A comprehensive testing framework for `sokybot-pk2` that enables compatibility testing against different game versions (private servers) and verification of well-known necessary files.

## Purpose

1. **Compatibility Testing**: Ensure the app's PK2 driver works correctly against different game versions/private servers
2. **File Existence Verification**: Verify that well-known necessary files exist in the PK2 archives
3. **Cross-Server Testing**: Support parameterized tests across multiple game directories (different private servers)

## Features

- Configurable game directory paths via system properties, environment variables, or text files
- Auto-discovery of all `.pk2` files in game directories
- Convenient access to Media.pk2, Data.pk2, Map.pk2, Music.pk2, Particles.pk2, and other PK2 files
- Custom assertions for PK2 compatibility and file verification
- Parameterized test support for testing across multiple game directories
- Optional JUnit 5 extension for automatic fixture injection
- Well-known files verification (required and optional files)
- **Comprehensive compatibility reporting** - Generate detailed reports about incompatibility issues for each game

## Configuration

### Single Game Directory

**Via System Property:**
```bash
mvn test -Dpk2.test.game.path=/path/to/game/directory
```

**Via Environment Variable:**
```bash
export PK2_TEST_GAME_PATH=/path/to/game/directory
mvn test
```

### Multiple Game Directories (Parameterized Tests)

**Via System Property (comma-separated):**
```bash
mvn test -Dpk2.test.game.paths=/path/to/server1,/path/to/server2,/path/to/server3
```

**Via Environment Variable (comma-separated):**
```bash
export PK2_TEST_GAME_PATHS=/path/to/server1,/path/to/server2,/path/to/server3
mvn test
```

**Via Text File (Recommended):**
```bash
# Create src/test/resources/pk2-test-game-paths.txt:
/path/to/server1
/path/to/server2
/path/to/server3
# This is a comment

# Then run tests - file will be automatically discovered
mvn test
```

**Via Custom Text File Path:**
```bash
mvn test -Dpk2.test.game.paths.file=/custom/path/to/game-directories.txt
```

Or via environment variable:
```bash
export PK2_TEST_GAME_PATHS_FILE=/custom/path/to/game-directories.txt
mvn test
```

## Text File Format

The text file (`pk2-test-game-paths.txt`) should contain one game directory path per line:
- Empty lines are ignored
- Lines starting with `#` are treated as comments
- Supports both absolute and relative paths
- Relative paths are resolved relative to project root

Example:
```
# Game directories for PK2 compatibility testing
# One path per line, empty lines and comments are ignored

C:\Games\Silkroad\Server1
C:\Games\Silkroad\Server2
D:\Silkroad\PrivateServer1
# /path/to/linux/server
/path/to/another/server
```

## Usage Examples

### Basic Compatibility Test

```java
import org.sokybot.pk2.test.*;
import org.sokybot.pk2.IPk2Driver;
import org.junit.jupiter.api.Test;

class MyPk2Test {
    private final Pk2TestFixture fixture = new Pk2TestFixture();
    
    @Test
    void testMediaPk2CanBeOpened() {
        IPk2Driver media = fixture.getMediaPk2();
        assertNotNull(media);
        assertTrue(media.find(".*").size() > 0);
    }
    
    @Test
    void testRequiredFilesExist() {
        Pk2Assertions.assertPk2ContainsRequiredFiles(fixture, "Media.pk2");
    }
    
    @Test
    void testPk2DriverCompatibility() {
        Pk2Assertions.assertPk2DriverCompatible(fixture);
    }
}
```

### Parameterized Test Across Multiple Servers

```java
import org.sokybot.pk2.test.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

class CrossServerCompatibilityTest {
    
    @ParameterizedTest
    @ArgumentsSource(Pk2GameDirectorySource.class)
    void testCompatibilityAcrossServers(String gameDirectory, Pk2TestFixture fixture) {
        // Test PK2 driver works with this game version
        Pk2Assertions.assertPk2DriverCompatible(fixture);
        
        // Verify required files exist
        Pk2Assertions.assertPk2ContainsRequiredFiles(fixture, "Media.pk2");
        
        // Test specific functionality
        IPk2Driver media = fixture.getMediaPk2();
        Optional<JMXFile> versionFile = media.findFirst("SV.T");
        assertTrue(versionFile.isPresent());
        
        // Cleanup
        fixture.close();
    }
}
```

### Using JUnit Extension

```java
import org.sokybot.pk2.test.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(Pk2TestExtension.class)
class Pk2TestWithExtension {
    
    @Pk2TestExtension.InjectPk2Fixture
    private Pk2TestFixture fixture;
    
    @Test
    void testWithInjectedFixture() {
        IPk2Driver media = fixture.getMediaPk2();
        // test logic
    }
}
```

### Accessing Different PK2 Files

```java
Pk2TestFixture fixture = new Pk2TestFixture();

// Required: Media.pk2
IPk2Driver media = fixture.getMediaPk2();

// Optional: Data.pk2, Map.pk2, Music.pk2, Particles.pk2
Optional<IPk2Driver> data = fixture.getDataPk2();
Optional<IPk2Driver> map = fixture.getMapPk2();
Optional<IPk2Driver> music = fixture.getMusicPk2();
Optional<IPk2Driver> particles = fixture.getParticlesPk2();

// Generic access for any PK2 file
Optional<IPk2Driver> customPk2 = fixture.getPk2("Custom.pk2");

// Get all available PK2 files
Map<String, IPk2Driver> allPk2Files = fixture.getAllPk2Files();
Set<String> availablePk2Files = fixture.getAvailablePk2Files();
```

### Custom Assertions

```java
// Assert PK2 can be opened
Pk2Assertions.assertPk2CanBeOpened(fixture, "Media.pk2");

// Assert required files exist
Pk2Assertions.assertPk2ContainsRequiredFiles(fixture, "Media.pk2");

// Assert specific file exists
Pk2Assertions.assertPk2ContainsFile(fixture, "Media.pk2", "itemdata.txt");

// Assert file size
Pk2Assertions.assertPk2FileSize(jmxFile, 1024);

// Assert file is readable
Pk2Assertions.assertPk2FileReadable(jmxFile);

// Comprehensive compatibility check
Pk2Assertions.assertPk2DriverCompatible(fixture);

// Verify all well-known files
Pk2Assertions.assertAllWellKnownFilesPresent(fixture);

// Custom content validation
Pk2Assertions.assertPk2FileContent(jmxFile, inputStream -> {
    // Validate content
});
```

## Well-Known Files Reference

### Media.pk2 Required Files

- `SV.T` - Game version file (encrypted)
- `divisioninfo.txt` - Division/server information
- `gateport.txt` - Gateway port configuration
- `type.txt` - Game type (e.g., Silkroad Online type)
- `characterdata.txt` - Character/NPC data
- `itemdata.txt` - Item definitions

### Media.pk2 Common Files (Optional)

- `skilldata*.txt` - Skill data files
- `regioninfo.txt` - Region/zone information
- `teleport*.txt` - Teleport data
- `portal*.txt` - Portal data
- `npcdata*.txt` - NPC data
- `reftext.txt` - Localization text
- `refpackageitem.txt` - Package item data
- `magicoption.txt` - Magic option data
- `teleportlink.txt` - Teleport link data
- `questdata.txt` - Quest data
- `questreward.txt` - Quest reward data
- `fullleveldata.txt` - Full level data

### Data.pk2 Common Files (Optional)

- `*.nvm` - Navmesh files for navigation

### Map.pk2, Music.pk2, Particles.pk2

These PK2 files have variable structures depending on the game version/server.

## Typical Game Directory Structure

The framework expects a directory structure like:
```
game-directory/
├── Media.pk2          (required)
├── Data.pk2           (common)
├── Map.pk2            (optional)
├── Music.pk2          (optional)
├── Particles.pk2      (optional)
├── silkroad.exe       (or similar executable)
└── [other files...]
```

## Compatibility Reporting

The framework includes a comprehensive reporting system that generates detailed compatibility reports for each game directory. Reports identify all incompatibility issues, missing files, and provide actionable information.

### Generating Reports

```java
import org.sokybot.pk2.test.*;

Pk2TestFixture fixture = new Pk2TestFixture();

// Generate compatibility report
Pk2CompatibilityReport report = Pk2CompatibilityReporter.generateReport(fixture);

// Print report to console
String textReport = Pk2CompatibilityReporter.generateTextReport(report);
System.out.println(textReport);

// Save report to file
Path reportPath = Pk2CompatibilityReporter.saveReport(report);
// Report saved to: target/compatibility-reports/compatibility-report_GameName_20240115_123456.txt
```

### Report Contents

The compatibility report includes:

- **Overall Compatibility Status**: YES/NO based on critical issues
- **Issue Summary**: Count of issues by severity (CRITICAL, WARNING, INFO)
- **PK2 File Analysis**: For each PK2 file:
  - Whether it can be opened
  - Total number of files
  - Required files found/missing
  - Optional files found/missing
  - List of missing files
- **Detailed Issues**: Full list of all issues with descriptions and details

### Report Example

```
================================================================================
PK2 COMPATIBILITY REPORT
================================================================================

Game Directory: /path/to/game
Timestamp: 2024-01-15T12:34:56
Compatible: NO
Total Issues: 5

Issues Summary:
  CRITICAL: 1
  WARNING:  4
  INFO:     0

--------------------------------------------------------------------------------
PK2 FILE ANALYSIS
--------------------------------------------------------------------------------

PK2 File: Media.pk2
  Can be opened: YES
  Total files: 1234
  Required files found: 5 / 6
  Optional files found: 8
  Missing required files:
    - gateport.txt

PK2 File: Data.pk2
  Can be opened: YES
  Total files: 567
  Required files found: 0 / 0
  Optional files found: 3

--------------------------------------------------------------------------------
DETAILED ISSUES
--------------------------------------------------------------------------------

CRITICAL ISSUES:
  [MISSING_REQUIRED_FILE] Missing required file: gateport.txt
      PK2: Media.pk2
      Details: Required file 'gateport.txt' not found in Media.pk2

WARNINGS:
  [MISSING_OPTIONAL_FILE] Missing optional file: skilldata*.txt
      PK2: Media.pk2
      Details: Optional file 'skilldata*.txt' not found in Media.pk2
```

### Using Reports in Tests

The `Pk2CompatibilityTest` class includes a test method that automatically generates reports:

```java
@Test
void testGenerateCompatibilityReport() throws Exception {
    Pk2CompatibilityReport report = Pk2CompatibilityReporter.generateReport(fixture);
    
    // Print to console
    String textReport = Pk2CompatibilityReporter.generateTextReport(report);
    log.info("\n{}", textReport);
    
    // Save to file
    Path reportPath = Pk2CompatibilityReporter.saveReport(report);
    
    // Access report data programmatically
    if (report.hasCriticalIssues()) {
        for (Pk2CompatibilityReport.Issue issue : report.getCriticalIssues()) {
            log.warn("CRITICAL: {} - {}", issue.getDescription(), issue.getDetails());
        }
    }
}
```

### Report File Location

Reports are automatically saved to:
```
target/compatibility-reports/compatibility-report_<GameDirectoryName>_<Timestamp>.txt
```

You can also specify a custom path:
```java
Pk2CompatibilityReporter.saveReport(report, "/custom/path/report.txt");
```

## Best Practices

1. **Use text files for multiple directories**: When testing against multiple game versions, use `pk2-test-game-paths.txt` for easier maintenance.

2. **Close fixtures properly**: Always close `Pk2TestFixture` instances to free resources:
   ```java
   try (Pk2TestFixture fixture = new Pk2TestFixture()) {
       // test logic
   }
   ```

3. **Handle optional PK2 files**: Always check if optional PK2 files (Data.pk2, Map.pk2, etc.) exist before using them:
   ```java
   Optional<IPk2Driver> data = fixture.getDataPk2();
   data.ifPresent(driver -> {
       // test logic
   });
   ```

4. **Use parameterized tests for compatibility**: Use `Pk2GameDirectorySource` to test across multiple game directories:
   ```java
   @ParameterizedTest
   @ArgumentsSource(Pk2GameDirectorySource.class)
   void test(String gameDirectory, Pk2TestFixture fixture) {
       // test logic
       fixture.close(); // Cleanup
   }
   ```

5. **Verify required files first**: Always verify that required files exist before testing specific functionality.

6. **Generate compatibility reports**: Use `Pk2CompatibilityReporter` to generate detailed reports for each game directory. Reports help identify all compatibility issues at once rather than failing on the first issue.

7. **Review reports for multiple games**: When testing multiple game directories, generate reports for each and compare them to identify common patterns or game-specific differences.

## Troubleshooting

**Error: "No game directory configured"**
- Set system property `pk2.test.game.path` or environment variable `PK2_TEST_GAME_PATH`
- Create `pk2-test-game-paths.txt` in test resources
- Ensure the configured directory exists and contains `Media.pk2`

**Error: "Media.pk2 not found"**
- Verify the game directory path is correct
- Ensure `Media.pk2` exists in the directory
- Check file permissions

**Parameterized tests not running**
- Ensure multiple game directories are configured
- Check that the paths file exists and is readable
- Verify directory paths are valid

## Components

- `Pk2TestConfiguration` - Configuration system for loading game directory paths
- `Pk2TestFixture` - Test fixture for accessing PK2 files
- `WellKnownFiles` - Definition of well-known files in PK2 archives
- `Pk2Assertions` - Custom assertions for PK2 testing
- `Pk2GameDirectorySource` - ArgumentsProvider for parameterized tests
- `Pk2TestExtension` - JUnit 5 extension for automatic fixture injection
- `Pk2CompatibilityTest` - Example compatibility test suite
- `Pk2CompatibilityReport` - Report model for compatibility issues
- `Pk2CompatibilityReporter` - Report generator and analyzer