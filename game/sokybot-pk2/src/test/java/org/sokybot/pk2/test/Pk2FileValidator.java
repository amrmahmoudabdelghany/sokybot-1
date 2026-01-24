package org.sokybot.pk2.test;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2.JMXFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Validates PK2 driver correctness by comparing files from PK2 archives
 * against their extracted filesystem counterparts using efficient methods.
 * 
 * @author sokybot
 */
@Slf4j
public class Pk2FileValidator {
    
    private final Pk2TestFixture fixture;
    private final MessageDigest digest;
    
    public Pk2FileValidator(Pk2TestFixture fixture) {
        this.fixture = fixture;
        try {
            this.digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
    
    /**
     * Validate a file using checksum comparison (fast).
     * 
     * @param pk2Name name of the PK2 file
     * @param filePath path to the file within the PK2
     * @return ValidationResult
     */
    public ValidationResult validateFile(String pk2Name, String filePath) {
        return validateFile(pk2Name, filePath, ValidationMode.CHECKSUM);
    }
    
    /**
     * Validate a file using the specified validation mode.
     * 
     * @param pk2Name name of the PK2 file
     * @param filePath path to the file within the PK2
     * @param mode validation mode (SIZE_ONLY, CHECKSUM, or BYTE_BYTE)
     * @return ValidationResult
     */
    public ValidationResult validateFile(String pk2Name, String filePath, ValidationMode mode) {
        Optional<JMXFile> pk2File = fixture.findFile(pk2Name, filePath);
        Optional<Path> extractedDir = fixture.getExtractedDirectory(pk2Name);
        
        if (pk2File.isEmpty()) {
            return ValidationResult.builder()
                .pk2Name(pk2Name)
                .filePath(filePath)
                .status(ValidationStatus.NOT_FOUND_IN_PK2)
                .message("File not found in PK2 archive")
                .build();
        }
        
        if (extractedDir.isEmpty()) {
            return ValidationResult.builder()
                .pk2Name(pk2Name)
                .filePath(filePath)
                .status(ValidationStatus.NO_EXTRACTED_DIRECTORY)
                .message("Extracted directory not available for comparison")
                .build();
        }
        
        Path extractedFilePath = findFileInDirectory(extractedDir.get(), filePath);
        if (extractedFilePath == null) {
            return ValidationResult.builder()
                .pk2Name(pk2Name)
                .filePath(filePath)
                .status(ValidationStatus.NOT_FOUND_IN_EXTRACTED)
                .message("File not found in extracted directory")
                .build();
        }
        
        // Compare file sizes first (always fast)
        long extractedSize;
        try {
            extractedSize = Files.size(extractedFilePath);
        } catch (IOException e) {
            return ValidationResult.builder()
                .pk2Name(pk2Name)
                .filePath(filePath)
                .status(ValidationStatus.ERROR)
                .message("Error reading extracted file: " + e.getMessage())
                .build();
        }
        
        int pk2Size = pk2File.get().getSize();
        if (pk2Size != extractedSize) {
            return ValidationResult.builder()
                .pk2Name(pk2Name)
                .filePath(filePath)
                .status(ValidationStatus.SIZE_MISMATCH)
                .message(String.format("Size mismatch: PK2=%d, Extracted=%d", pk2Size, extractedSize))
                .pk2Size((long) pk2Size)
                .extractedSize(extractedSize)
                .build();
        }
        
        // If SIZE_ONLY mode, we're done
        if (mode == ValidationMode.SIZE_ONLY) {
            return ValidationResult.builder()
                .pk2Name(pk2Name)
                .filePath(filePath)
                .status(ValidationStatus.VALID)
                .message("File sizes match")
                .pk2Size((long) pk2Size)
                .extractedSize(extractedSize)
                .build();
        }
        
        // Compare checksums (fast for CHECKSUM mode)
        if (mode == ValidationMode.CHECKSUM) {
            try {
                String pk2Checksum = calculateChecksum(pk2File.get());
                String extractedChecksum = calculateChecksum(extractedFilePath);
                
                if (!pk2Checksum.equals(extractedChecksum)) {
                    return ValidationResult.builder()
                        .pk2Name(pk2Name)
                        .filePath(filePath)
                        .status(ValidationStatus.CONTENT_MISMATCH)
                        .message("File checksums do not match")
                        .pk2Size((long) pk2Size)
                        .extractedSize(extractedSize)
                        .pk2Checksum(pk2Checksum)
                        .extractedChecksum(extractedChecksum)
                        .build();
                }
                
                return ValidationResult.builder()
                    .pk2Name(pk2Name)
                    .filePath(filePath)
                    .status(ValidationStatus.VALID)
                    .message("File checksums match")
                    .pk2Size((long) pk2Size)
                    .extractedSize(extractedSize)
                    .build();
                    
            } catch (IOException e) {
                return ValidationResult.builder()
                    .pk2Name(pk2Name)
                    .filePath(filePath)
                    .status(ValidationStatus.ERROR)
                    .message("Error calculating checksums: " + e.getMessage())
                    .build();
            }
        }
        
        // BYTE_BYTE mode (only for small files or samples)
        if (mode == ValidationMode.BYTE_BYTE) {
            try {
                boolean contentsMatch = compareFileContents(pk2File.get(), extractedFilePath);
                if (!contentsMatch) {
                    return ValidationResult.builder()
                        .pk2Name(pk2Name)
                        .filePath(filePath)
                        .status(ValidationStatus.CONTENT_MISMATCH)
                        .message("File contents do not match")
                        .pk2Size((long) pk2Size)
                        .extractedSize(extractedSize)
                        .build();
                }
                
                return ValidationResult.builder()
                    .pk2Name(pk2Name)
                    .filePath(filePath)
                    .status(ValidationStatus.VALID)
                    .message("File contents match perfectly")
                    .pk2Size((long) pk2Size)
                    .extractedSize(extractedSize)
                    .build();
            } catch (IOException e) {
                return ValidationResult.builder()
                    .pk2Name(pk2Name)
                    .filePath(filePath)
                    .status(ValidationStatus.ERROR)
                    .message("Error comparing file contents: " + e.getMessage())
                    .build();
            }
        }
        
        throw new IllegalArgumentException("Unknown validation mode: " + mode);
    }
    
    /**
     * Validate well-known required files efficiently (size + checksum for small files).
     * 
     * @param pk2Name name of the PK2 file
     * @return list of validation results
     */
    public List<ValidationResult> validateRequiredFiles(String pk2Name) {
        Set<String> requiredFiles = WellKnownFiles.getRequiredFiles(pk2Name);
        List<ValidationResult> results = new ArrayList<>();
        
        for (String filePath : requiredFiles) {
            // Use checksum validation for required files (they're usually small)
            ValidationResult result = validateFile(pk2Name, filePath, ValidationMode.CHECKSUM);
            results.add(result);
        }
        
        return results;
    }
    
    /**
     * Validate a sample of files from PK2 archive using statistical sampling.
     * 
     * @param pk2Name name of the PK2 file
     * @param sampleSize number of files to sample (or -1 for all)
     * @param maxFileSize maximum file size to include in sample (bytes)
     * @return validation summary
     */
    public ValidationSummary validateSample(String pk2Name, int sampleSize, long maxFileSize) {
        Optional<IPk2Driver> driver = fixture.getPk2(pk2Name);
        if (driver.isEmpty()) {
            return ValidationSummary.builder()
                .pk2Name(pk2Name)
                .status(ValidationStatus.NO_EXTRACTED_DIRECTORY)
                .message("PK2 file not found")
                .build();
        }
        
        Optional<Path> extractedDir = fixture.getExtractedDirectory(pk2Name);
        if (extractedDir.isEmpty()) {
            return ValidationSummary.builder()
                .pk2Name(pk2Name)
                .status(ValidationStatus.NO_EXTRACTED_DIRECTORY)
                .message("Extracted directory not found")
                .build();
        }
        
        // Get all files from PK2
        List<JMXFile> allFiles = driver.get().find(".*");
        
        // Filter by size and sample
        List<JMXFile> sampleFiles = allFiles.stream()
            .filter(f -> f.getSize() <= maxFileSize)
            .limit(sampleSize > 0 ? sampleSize : allFiles.size())
            .collect(Collectors.toList());
        
        int validated = 0;
        int valid = 0;
        int sizeMismatch = 0;
        int contentMismatch = 0;
        int notFound = 0;
        int errors = 0;
        
        for (JMXFile jmxFile : sampleFiles) {
            ValidationResult result = validateFile(pk2Name, jmxFile.getName(), ValidationMode.CHECKSUM);
            validated++;
            
            switch (result.getStatus()) {
                case VALID:
                    valid++;
                    break;
                case SIZE_MISMATCH:
                    sizeMismatch++;
                    break;
                case CONTENT_MISMATCH:
                    contentMismatch++;
                    break;
                case NOT_FOUND_IN_EXTRACTED:
                    notFound++;
                    break;
                default:
                    errors++;
            }
        }
        
        return ValidationSummary.builder()
            .pk2Name(pk2Name)
            .totalFiles(allFiles.size())
            .sampledFiles(sampleFiles.size())
            .validatedFiles(validated)
            .validFiles(valid)
            .sizeMismatches(sizeMismatch)
            .contentMismatches(contentMismatch)
            .notFoundInExtracted(notFound)
            .errors(errors)
            .status(validated > 0 && errors == 0 && contentMismatch == 0 && sizeMismatch == 0 
                ? ValidationStatus.VALID 
                : ValidationStatus.PARTIAL_VALIDATION)
            .message(String.format(
                "Validated %d/%d files: %d valid, %d size mismatches, %d content mismatches",
                validated, allFiles.size(), valid, sizeMismatch, contentMismatch))
            .build();
    }
    
    /**
     * Calculate MD5 checksum of a file.
     */
    private String calculateChecksum(JMXFile jmxFile) throws IOException {
        synchronized (digest) {
            digest.reset();
            try (InputStream is = jmxFile.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            return bytesToHex(digest.digest());
        }
    }
    
    /**
     * Calculate MD5 checksum of a filesystem file.
     */
    private String calculateChecksum(Path filePath) throws IOException {
        synchronized (digest) {
            digest.reset();
            try (InputStream is = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            return bytesToHex(digest.digest());
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    private Path findFileInDirectory(Path rootDir, String filePath) {
        String normalizedPath = filePath.replace("\\", "/");
        Path targetPath = rootDir.resolve(normalizedPath);
        
        if (Files.exists(targetPath) && Files.isRegularFile(targetPath)) {
            return targetPath;
        }
        return null;
    }
    
    private boolean compareFileContents(JMXFile pk2File, Path extractedFile) throws IOException {
        try (InputStream pk2Stream = pk2File.getInputStream();
             InputStream extractedStream = Files.newInputStream(extractedFile)) {
            
            byte[] pk2Buffer = new byte[8192];
            byte[] extractedBuffer = new byte[8192];
            
            while (true) {
                int pk2Read = pk2Stream.read(pk2Buffer);
                int extractedRead = extractedStream.read(extractedBuffer);
                
                if (pk2Read != extractedRead) {
                    return false;
                }
                
                if (pk2Read == -1) {
                    break;
                }
                
                if (!Arrays.equals(pk2Buffer, extractedBuffer)) {
                    return false;
                }
            }
            
            return true;
        }
    }
    
    @Getter
    @Builder
    public static class ValidationResult {
        private final String pk2Name;
        private final String filePath;
        private final ValidationStatus status;
        private final String message;
        private final Long pk2Size;
        private final Long extractedSize;
        private final String pk2Checksum;
        private final String extractedChecksum;
        
        public boolean isValid() {
            return status == ValidationStatus.VALID;
        }
    }
    
    @Getter
    @Builder
    public static class ValidationSummary {
        private final String pk2Name;
        private final ValidationStatus status;
        private final String message;
        private final int totalFiles;
        private final int sampledFiles;
        private final int validatedFiles;
        private final int validFiles;
        private final int sizeMismatches;
        private final int contentMismatches;
        private final int notFoundInExtracted;
        private final int errors;
    }
    
    public enum ValidationStatus {
        VALID,
        NOT_FOUND_IN_PK2,
        NOT_FOUND_IN_EXTRACTED,
        NO_EXTRACTED_DIRECTORY,
        SIZE_MISMATCH,
        CONTENT_MISMATCH,
        PARTIAL_VALIDATION,
        ERROR
    }
    
    public enum ValidationMode {
        SIZE_ONLY,      // Fastest: only compare file sizes
        CHECKSUM,       // Fast: compare MD5 checksums
        BYTE_BYTE       // Slow: full byte-by-byte comparison (use sparingly)
    }
}
