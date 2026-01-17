package org.sokybot.pk2.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2.JMXFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Custom assertions for PK2 compatibility and file verification.
 * Provides fluent assertion API for testing PK2 files and their contents.
 * 
 * @author sokybot
 */
@Slf4j
public class Pk2Assertions {
    
    /**
     * Assert that a PK2 file can be opened successfully.
     * 
     * @param fixture the test fixture
     * @param pk2Name name of the PK2 file (e.g., "Media.pk2")
     */
    public static void assertPk2CanBeOpened(Pk2TestFixture fixture, String pk2Name) {
        if (fixture.getPk2(pk2Name).isEmpty()) {
            Assertions.fail(String.format("PK2 file '%s' could not be opened in directory: %s", 
                pk2Name, fixture.getGameDirectory()));
        }
    }
    
    /**
     * Assert that all required files exist in a PK2 archive.
     * 
     * @param fixture the test fixture
     * @param pk2Name name of the PK2 file (e.g., "Media.pk2")
     */
    public static void assertPk2ContainsRequiredFiles(Pk2TestFixture fixture, String pk2Name) {
        Set<String> requiredFiles = WellKnownFiles.getRequiredFiles(pk2Name);
        
        if (requiredFiles.isEmpty()) {
            log.debug("No required files defined for PK2: {}", pk2Name);
            return;
        }
        
        IPk2Driver driver = fixture.getPk2(pk2Name)
            .orElseGet(() -> {
                Assertions.fail(String.format("PK2 file '%s' not found in directory: %s", 
                    pk2Name, fixture.getGameDirectory()));
                return null; // Never reached
            });
        
        List<String> missingFiles = requiredFiles.stream()
            .filter(requiredFile -> {
                // Try exact match first
                if (driver.findFirst(requiredFile).isPresent()) {
                    return false;
                }
                // Try with regex pattern (handle wildcards and case-insensitive patterns)
                String pattern = requiredFile;
                // If it's already a regex pattern (starts with (?i) or contains regex chars), use as-is
                if (!pattern.startsWith("(?i)") && !pattern.matches(".*[\\[\\]\\(\\)\\{\\}\\|\\+\\?\\^\\$].*")) {
                    // Convert simple wildcard pattern to regex
                    pattern = requiredFile.replace("*", ".*").replace(".", "\\.");
                }
                return driver.find(pattern).isEmpty();
            })
            .toList();
        
        if (!missingFiles.isEmpty()) {
            Assertions.fail(String.format(
                "PK2 file '%s' is missing required files: %s",
                pk2Name, missingFiles));
        }
    }
    
    /**
     * Assert that a specific file exists in a PK2 archive.
     * 
     * @param fixture the test fixture
     * @param pk2Name name of the PK2 file
     * @param filePath path to the file within the PK2 (can be regex)
     */
    public static void assertPk2ContainsFile(Pk2TestFixture fixture, String pk2Name, String filePath) {
        boolean found = fixture.findFile(pk2Name, filePath).isPresent();
        
        Assertions.assertTrue(found, String.format(
            "PK2 file '%s' does not contain file matching pattern: %s",
            pk2Name, filePath));
    }
    
    /**
     * Assert that a JMX file has the expected size.
     * 
     * @param jmxFile the JMX file to check
     * @param expectedSize expected file size in bytes
     */
    public static void assertPk2FileSize(JMXFile jmxFile, int expectedSize) {
        Assertions.assertEquals(expectedSize, jmxFile.getSize(), 
            String.format("JMX file '%s' size mismatch", jmxFile.getName()));
    }
    
    /**
     * Assert that a JMX file can be read.
     * 
     * @param jmxFile the JMX file to check
     */
    public static void assertPk2FileReadable(JMXFile jmxFile) {
        try (InputStream is = jmxFile.getInputStream()) {
            Assertions.assertNotNull(is, 
                String.format("Cannot get input stream for JMX file: %s", jmxFile.getName()));
            // Try to read at least one byte
            int firstByte = is.read();
            Assertions.assertTrue(firstByte >= 0 || jmxFile.getSize() == 0,
                String.format("JMX file '%s' appears to be empty but size is %d", 
                    jmxFile.getName(), jmxFile.getSize()));
        } catch (IOException e) {
            Assertions.fail(String.format("Error reading JMX file '%s': %s", 
                jmxFile.getName(), e.getMessage()), e);
        }
    }
    
    /**
     * Assert PK2 driver compatibility by opening all PK2 files and reading headers.
     * 
     * @param fixture the test fixture
     */
    public static void assertPk2DriverCompatible(Pk2TestFixture fixture) {
        Set<String> availablePk2Files = fixture.getAvailablePk2Files();
        
        if (availablePk2Files.isEmpty()) {
            Assertions.fail(String.format(
                "No PK2 files found in game directory: %s", 
                fixture.getGameDirectory()));
        }
        
        for (String pk2Name : availablePk2Files) {
            Optional<IPk2Driver> driverOpt = fixture.getPk2(pk2Name);
            if (driverOpt.isEmpty()) {
                Assertions.fail(String.format("Failed to open PK2 file: %s", pk2Name));
                return; // Never reached
            }
            IPk2Driver driver = driverOpt.get();
            
            // Try to find at least one file to verify the PK2 is readable
            try {
                List<JMXFile> files = driver.find(".*", 1);
                Assertions.assertFalse(files.isEmpty(),
                    String.format("PK2 file '%s' appears to be empty or unreadable", pk2Name));
            } catch (Exception e) {
                Assertions.fail(String.format(
                    "Error accessing PK2 file '%s': %s", pk2Name, e.getMessage()), e);
            }
        }
        
        log.debug("PK2 driver compatibility verified for {} PK2 files", availablePk2Files.size());
    }
    
    /**
     * Assert that all well-known files (required + optional) are present across all PK2 files.
     * This is a comprehensive check that verifies file existence but allows optional files to be missing.
     * 
     * @param fixture the test fixture
     */
    public static void assertAllWellKnownFilesPresent(Pk2TestFixture fixture) {
        for (String pk2Name : fixture.getAvailablePk2Files()) {
            // Check required files (these must exist)
            assertPk2ContainsRequiredFiles(fixture, pk2Name);
            
            // Optional files are logged but don't fail the test
            Set<String> optionalFiles = WellKnownFiles.getOptionalFiles(pk2Name);
            for (String optionalFile : optionalFiles) {
                boolean present = fixture.isWellKnownFilePresent(pk2Name, optionalFile);
                if (!present) {
                    log.debug("Optional file '{}' not found in PK2: {}", optionalFile, pk2Name);
                }
            }
        }
    }
    
    /**
     * Assert file content using a custom consumer.
     * 
     * @param jmxFile the JMX file to check
     * @param contentConsumer consumer that validates the file content
     */
    public static void assertPk2FileContent(JMXFile jmxFile, Consumer<InputStream> contentConsumer) {
        try (InputStream is = jmxFile.getInputStream()) {
            contentConsumer.accept(is);
        } catch (IOException e) {
            Assertions.fail(String.format("Error reading content of JMX file '%s': %s", 
                jmxFile.getName(), e.getMessage()), e);
        }
    }
    
    /**
     * Assert that a file from PK2 archive matches its extracted filesystem version.
     * 
     * @param fixture the test fixture
     * @param pk2Name name of the PK2 file
     * @param filePath path to the file within the PK2
     */
    public static void assertPk2FileMatchesExtracted(Pk2TestFixture fixture, 
                                                     String pk2Name, 
                                                     String filePath) {
        Pk2FileValidator validator = new Pk2FileValidator(fixture);
        Pk2FileValidator.ValidationResult result = validator.validateFile(pk2Name, filePath);
        
        Assertions.assertTrue(result.isValid(),
            () -> String.format(
                "File '%s' from '%s' does not match extracted version. Status: %s, Message: %s",
                filePath, pk2Name, result.getStatus(), result.getMessage()));
    }
    
    /**
     * Assert that a well-known required file from PK2 matches its extracted version.
     * 
     * @param fixture the test fixture
     * @param pk2Name name of the PK2 file
     * @param filePath path to the file within the PK2
     */
    public static void assertRequiredFileMatchesExtracted(Pk2TestFixture fixture,
                                                     String pk2Name,
                                                     String filePath) {
        // First check file exists
        assertPk2ContainsFile(fixture, pk2Name, filePath);
        
        // Then validate it matches extracted version
        assertPk2FileMatchesExtracted(fixture, pk2Name, filePath);
    }
}