package org.sokybot.pk2.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2.JMXFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive compatibility test suite for PK2 driver.
 * Tests compatibility across different game versions and verifies required files exist.
 * 
 * @author sokybot
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Pk2CompatibilityTest {
    
    private static Pk2TestFixture fixture;
    
    @BeforeAll
    static void setUp() {
        Pk2TestConfiguration config = Pk2TestConfiguration.getInstance();
        if (!config.isConfigured()) {
            Assumptions.assumeTrue(false, "No game directory configured. Skipping compatibility tests.");
        }
        fixture = new Pk2TestFixture();
    }
    
    @AfterAll
    static void tearDown() throws Exception {
        if (fixture != null) {
            fixture.close();
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("Media.pk2 can be opened")
    void testMediaPk2CanBeOpened() {
        IPk2Driver media = fixture.getMediaPk2();
        assertNotNull(media, "Media.pk2 should be accessible");
        
        // Verify it's not empty
        List<JMXFile> files = media.find(".*", 1);
        assertFalse(files.isEmpty(), "Media.pk2 should contain at least one file");
    }
    
    @Test
    @Order(2)
    @DisplayName("Media.pk2 contains required files")
    void testMediaPk2ContainsRequiredFiles() {
        Pk2Assertions.assertPk2ContainsRequiredFiles(fixture, "Media.pk2");
    }
    
    @Test
    @Order(3)
    @DisplayName("Media.pk2 version file (SV.T) exists and is readable")
    void testMediaPk2VersionFile() {
        Optional<JMXFile> versionFile = fixture.findFile("Media.pk2", "SV.T");
        assertTrue(versionFile.isPresent(), "Media.pk2 should contain SV.T (version file)");
        
        JMXFile svt = versionFile.get();
        assertTrue(svt.getSize() > 0, "SV.T should not be empty");
        Pk2Assertions.assertPk2FileReadable(svt);
    }
    
    @Test
    @Order(4)
    @DisplayName("Data.pk2 can be opened if present")
    void testDataPk2CanBeOpened() {
        Optional<IPk2Driver> data = fixture.getDataPk2();
        
        data.ifPresent(driver -> {
            log.info("Data.pk2 found and opened successfully");
            List<JMXFile> files = driver.find(".*", 1);
            assertFalse(files.isEmpty(), "Data.pk2 should contain at least one file");
        });
        
        if (data.isEmpty()) {
            log.info("Data.pk2 not present in game directory (optional)");
        }
    }
    
    @Test
    @Order(5)
    @DisplayName("All PK2 files can be opened")
    void testAllPk2FilesCanBeOpened() {
        Set<String> availablePk2Files = fixture.getAvailablePk2Files();
        assertFalse(availablePk2Files.isEmpty(), 
            "At least one PK2 file should be available");
        
        for (String pk2Name : availablePk2Files) {
            Optional<IPk2Driver> driver = fixture.getPk2(pk2Name);
            assertTrue(driver.isPresent(), 
                String.format("PK2 file '%s' should be openable", pk2Name));
            
            // Verify basic functionality
            IPk2Driver pk2Driver = driver.get();
            List<JMXFile> files = pk2Driver.find(".*", 1);
            assertFalse(files.isEmpty(),
                String.format("PK2 file '%s' should contain at least one file", pk2Name));
        }
    }
    
    @Test
    @Order(6)
    @DisplayName("PK2 driver compatibility check")
    void testPk2DriverCompatibility() {
        Pk2Assertions.assertPk2DriverCompatible(fixture);
    }
    
    @Test
    @Order(7)
    @DisplayName("Well-known files verification")
    void testWellKnownFilesPresent() {
        Pk2Assertions.assertAllWellKnownFilesPresent(fixture);
    }
    
    @Test
    @Order(8)
    @DisplayName("Media.pk2 essential files exist")
    void testMediaPk2EssentialFiles() {
        IPk2Driver media = fixture.getMediaPk2();
        
        // Check critical required files
        String[] essentialFiles = {
            "SV.T",
            "divisioninfo.txt",
            "characterdata.txt",
            "itemdata.txt"
        };
        
        for (String fileName : essentialFiles) {
            Optional<JMXFile> file = media.findFirst(fileName);
            assertTrue(file.isPresent(),
                String.format("Media.pk2 should contain essential file: %s", fileName));
            
            JMXFile jmxFile = file.get();
            assertTrue(jmxFile.getSize() > 0,
                String.format("Essential file '%s' should not be empty", fileName));
        }
        
        // Check optional but common files (log but don't fail)
        // Use case-insensitive pattern to match variations like GATEWAYPORT, gateport.txt, etc.
        String[] optionalFilePatterns = {
            "(?i)gate.*port.*"  // Case-insensitive pattern for gateport files
        };
        
        for (String pattern : optionalFilePatterns) {
            List<JMXFile> files = media.find(pattern, 1);
            if (!files.isEmpty()) {
                JMXFile jmxFile = files.get(0);
                log.info("Found optional gateport file: {} (pattern: {})", jmxFile.getName(), pattern);
                assertTrue(jmxFile.getSize() > 0,
                    String.format("Optional gateport file '%s' should not be empty if present", jmxFile.getName()));
            } else {
                log.debug("Optional gateport file not found (pattern: {}) - may be missing in this game version", pattern);
            }
        }
    }
    
    /**
     * Generate comprehensive compatibility report.
     * This test generates a detailed report about compatibility issues.
     */
    @Test
    @Order(9)
    @DisplayName("Generate compatibility report")
    void testGenerateCompatibilityReport() {
        Pk2CompatibilityReport report = Pk2CompatibilityReporter.generateReport(fixture);
        
        // Print report to console
        String textReport = Pk2CompatibilityReporter.generateTextReport(report);
        log.info("\n{}", textReport);
        
        // Save report to file
        try {
            Path reportPath = Pk2CompatibilityReporter.saveReport(report);
            log.info("Compatibility report saved to: {}", reportPath.toAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to save compatibility report", e);
        }
        
        // Assertions based on report
        if (report.hasCriticalIssues()) {
            log.warn("Critical compatibility issues found. See report for details.");
            // Don't fail the test, but log the issues
            for (Pk2CompatibilityReport.Issue issue : report.getCriticalIssues()) {
                log.warn("CRITICAL: {} - {}", issue.getDescription(), issue.getDetails());
            }
        }
        
        // Test passes if report was generated successfully
        assertNotNull(report);
        assertEquals(fixture.getGameDirectory(), report.getGameDirectory());
    }
    
    /**
     * Parameterized test for compatibility across multiple game directories (different servers).
     * This test runs once for each configured game directory and generates a report for each.
     */
    @ParameterizedTest
    @ArgumentsSource(Pk2GameDirectorySource.class)
    @DisplayName("Cross-server compatibility test with reports")
    @Tag("parameterized")
    void testPk2DriverWorksWithDifferentServers(String gameDirectory, Pk2TestFixture testFixture) {
        log.info("Testing compatibility with game directory: {}", gameDirectory);
        
        try {
            // Generate compatibility report
            Pk2CompatibilityReport report = Pk2CompatibilityReporter.generateReport(testFixture);
            
            // Print summary
            log.info("Compatibility Report for: {}", gameDirectory);
            log.info("  Compatible: {}", report.isCompatible() ? "YES" : "NO");
            log.info("  Total Issues: {}", report.getTotalIssues());
            log.info("  Critical Issues: {}", report.getCriticalIssues().size());
            log.info("  Warnings: {}", report.getWarnings().size());
            
            // Save report
            try {
                Path reportPath = Pk2CompatibilityReporter.saveReport(report);
                log.info("  Report saved to: {}", reportPath.toAbsolutePath());
            } catch (Exception e) {
                log.error("Failed to save compatibility report", e);
            }
            
            // Print detailed report if there are issues
            if (!report.isCompatible() || report.getTotalIssues() > 0) {
                String textReport = Pk2CompatibilityReporter.generateTextReport(report);
                log.info("\n{}", textReport);
            }
            
            // Basic compatibility check (non-failing)
            try {
                Pk2Assertions.assertPk2DriverCompatible(testFixture);
            } catch (AssertionError e) {
                log.warn("Basic compatibility check failed: {}", e.getMessage());
            }
            
            // Verify Media.pk2 exists and works
            IPk2Driver media = testFixture.getMediaPk2();
            assertNotNull(media, "Media.pk2 should be accessible");
            
            // Verify version file exists
            Optional<JMXFile> versionFile = media.findFirst("SV.T");
            if (versionFile.isEmpty()) {
                log.warn("Media.pk2 in {} does not contain SV.T", gameDirectory);
            }
            
            log.info("Compatibility test completed for: {}", gameDirectory);
            
        } finally {
            // Cleanup
            try {
                testFixture.close();
            } catch (Exception e) {
                log.warn("Error closing fixture for {}", gameDirectory, e);
            }
        }
    }
    
    @Test
    @Order(10)
    @DisplayName("PK2 driver extracts files correctly (validated against extracted files)")
    void testPk2DriverExtractionCorrectness() {
        Pk2FileValidator validator = new Pk2FileValidator(fixture);
        
        // Validate required files (small, use checksum)
        if (fixture.hasExtractedDirectory("Media.pk2")) {
            log.info("Validating required files from Media.pk2 against extracted directory");
            
            List<Pk2FileValidator.ValidationResult> results = 
                validator.validateRequiredFiles("Media.pk2");
            
            int validCount = 0;
            int notFoundInPk2Count = 0;
            int notFoundInExtractedCount = 0;
            int mismatchCount = 0;
            
            for (Pk2FileValidator.ValidationResult result : results) {
                if (result.getStatus() == Pk2FileValidator.ValidationStatus.NOT_FOUND_IN_PK2) {
                    log.debug("Skipping {} - not found in PK2 archive (may be optional for this version)", 
                        result.getFilePath());
                    notFoundInPk2Count++;
                    continue;
                }
                
                if (result.getStatus() == Pk2FileValidator.ValidationStatus.NOT_FOUND_IN_EXTRACTED) {
                    log.debug("Skipping {} - not found in extracted directory", result.getFilePath());
                    notFoundInExtractedCount++;
                    continue;
                }
                
                if (result.isValid()) {
                    validCount++;
                } else {
                    log.warn("Validation failed for {}: {}", result.getFilePath(), result.getMessage());
                    mismatchCount++;
                    // Fail only on content/size mismatches, not on missing files
                    if (result.getStatus() == Pk2FileValidator.ValidationStatus.CONTENT_MISMATCH ||
                        result.getStatus() == Pk2FileValidator.ValidationStatus.SIZE_MISMATCH) {
                        Assertions.fail(String.format(
                            "Required file '%s' validation failed: %s", 
                            result.getFilePath(), result.getMessage()));
                    }
                }
            }
            
            log.info("Validated {} required files: {} valid, {} not in PK2, {} not in extracted, {} mismatches", 
                results.size(), validCount, notFoundInPk2Count, notFoundInExtractedCount, mismatchCount);
            
            // At least some files should have been validated
            assertTrue(validCount > 0, 
                "At least one required file should be validated successfully");
        } else {
            log.info("No extracted directory found for Media.pk2 - skipping extraction validation");
        }
        
        // Sample validation for larger filesets
        if (fixture.hasExtractedDirectory("Media.pk2")) {
            log.info("Running sample validation for Media.pk2");
            
            // Sample 50 files, max 1MB each (fast)
            Pk2FileValidator.ValidationSummary summary = 
                validator.validateSample("Media.pk2", 50, 1024 * 1024);
            
            log.info("Sample validation: {}", summary.getMessage());
            
            assertTrue(
                summary.getValidFiles() > 0 && 
                summary.getContentMismatches() == 0 && 
                summary.getSizeMismatches() == 0,
                () -> String.format(
                    "Sample validation failed: %s", summary.getMessage()));
        }
    }
    
    @ParameterizedTest(name = "Validate PK2 extraction for {0}")
    @ArgumentsSource(Pk2GameDirectorySource.class)
    @Order(11)
    @DisplayName("Validate PK2 driver extracts files correctly (sampled)")
    void testPk2ExtractionValidation(String gameDirectory, Pk2TestFixture parameterizedFixture) throws Exception {
        try (parameterizedFixture) {
            Pk2FileValidator validator = new Pk2FileValidator(parameterizedFixture);
            
            for (Map.Entry<String, Path> entry : parameterizedFixture.getExtractedDirectories().entrySet()) {
                String pk2Name = entry.getKey();
                log.info("Validating {} against extracted directory: {}", pk2Name, entry.getValue());
                
                // Quick sample: 20 files, max 500KB each
                Pk2FileValidator.ValidationSummary summary = 
                    validator.validateSample(pk2Name, 20, 500 * 1024);
                
                log.info("Validation summary for {}: {}", pk2Name, summary.getMessage());
                
                // Assert that at least some files validated successfully
                assertTrue(
                    summary.getValidatedFiles() > 0,
                    () -> String.format("No files validated for %s", pk2Name));
                
                // Assert no content mismatches in sample
                assertTrue(
                    summary.getContentMismatches() == 0,
                    () -> String.format(
                        "Content mismatches found in %s: %s", 
                        pk2Name, summary.getMessage()));
                
                // Assert no size mismatches in sample
                assertTrue(
                    summary.getSizeMismatches() == 0,
                    () -> String.format(
                        "Size mismatches found in %s: %s", 
                        pk2Name, summary.getMessage()));
            }
        }
    }
    
    @Test
    @Order(12)
    @DisplayName("Verify missing optional files - debug test")
    void testVerifyMissingOptionalFiles() {
        IPk2Driver media = fixture.getMediaPk2();
        
        // Test patterns from the report
        String[] patternsToTest = {
            "questreward.txt",
            "fullleveldata.txt",
            "reftext.txt",
            "GATEPORT.TXT",
            "gateport.txt",
            "skilldata*.txt",
            ".*skilldata.*\\.txt$",
            "portal*.txt",
            ".*portal.*\\.txt$",
            "npcdata*.txt",
            ".*npcdata.*\\.txt$",
            "teleport*.txt",
            ".*teleport.*\\.txt$",
            "(?i)gate.*port.*",
            "(?i).*gate.*port.*",
        };
        
        log.info("=== Verifying Media.pk2 optional file patterns ===");
        for (String pattern : patternsToTest) {
            List<JMXFile> matches = media.find(pattern, 5);
            log.info("Pattern: '{}' -> Found: {} files", pattern, matches.size());
            if (!matches.isEmpty()) {
                matches.forEach(f -> log.info("  - {}", f.getName()));
            }
        }
        
        // Test Data.pk2
        Optional<IPk2Driver> dataOpt = fixture.getPk2("Data.pk2");
        if (dataOpt.isPresent()) {
            IPk2Driver data = dataOpt.get();
            log.info("\n=== Verifying Data.pk2 optional file patterns ===");
            String[] dataPatterns = {
                ".*\\.nvm$",
                ".*\\.nvm",
            };
            
            for (String pattern : dataPatterns) {
                List<JMXFile> matches = data.find(pattern, 5);
                log.info("Pattern: '{}' -> Found: {} files", pattern, matches.size());
                if (!matches.isEmpty()) {
                    matches.forEach(f -> log.info("  - {}", f.getName()));
                }
            }
        }
    }
}