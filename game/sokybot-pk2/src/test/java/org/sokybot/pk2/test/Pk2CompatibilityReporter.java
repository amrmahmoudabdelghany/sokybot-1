package org.sokybot.pk2.test;

import lombok.extern.slf4j.Slf4j;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2.JMXFile;
import org.sokybot.pk2.test.Pk2CompatibilityReport.Issue;
import org.sokybot.pk2.test.Pk2CompatibilityReport.Pk2FileReport;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates compatibility reports for PK2 testing.
 * 
 * @author sokybot
 */
@Slf4j
public class Pk2CompatibilityReporter {
    
    /**
     * Generate a compatibility report for a game directory.
     * 
     * @param fixture the test fixture
     * @return compatibility report
     */
    public static Pk2CompatibilityReport generateReport(Pk2TestFixture fixture) {
        String gameDirectory = fixture.getGameDirectory();
        List<Issue> allIssues = new ArrayList<>();
        Map<String, Pk2FileReport> pk2FileReports = new HashMap<>();
        
        // Analyze each PK2 file
        for (String pk2Name : fixture.getAvailablePk2Files()) {
            Pk2FileReport fileReport = analyzePk2File(fixture, pk2Name);
            pk2FileReports.put(pk2Name, fileReport);
            allIssues.addAll(fileReport.getIssues());
        }
        
        // Determine overall compatibility
        boolean compatible = allIssues.stream()
            .noneMatch(issue -> "CRITICAL".equals(issue.getSeverity()));
        
        return Pk2CompatibilityReport.builder()
            .gameDirectory(gameDirectory)
            .timestamp(LocalDateTime.now())
            .compatible(compatible)
            .issues(allIssues)
            .pk2FileReports(pk2FileReports)
            .build();
    }
    
    /**
     * Analyze a specific PK2 file.
     */
    private static Pk2FileReport analyzePk2File(Pk2TestFixture fixture, String pk2Name) {
        List<Issue> issues = new ArrayList<>();
        List<String> missingRequiredFiles = new ArrayList<>();
        List<String> missingOptionalFiles = new ArrayList<>();
        List<String> foundFiles = new ArrayList<>();
        
        // Try to open the PK2 file
        Optional<IPk2Driver> driverOpt = fixture.getPk2(pk2Name);
        boolean canBeOpened = driverOpt.isPresent();
        
        if (!canBeOpened) {
            issues.add(Issue.builder()
                .type(Issue.IssueType.PK2_CANNOT_BE_OPENED)
                .pk2File(pk2Name)
                .description("Cannot open PK2 file")
                .severity("CRITICAL")
                .details("Failed to open " + pk2Name)
                .build());
            
            return Pk2FileReport.builder()
                .pk2FileName(pk2Name)
                .canBeOpened(false)
                .totalFiles(0)
                .requiredFilesFound(0)
                .requiredFilesMissing(0)
                .optionalFilesFound(0)
                .missingRequiredFiles(Collections.emptyList())
                .missingOptionalFiles(Collections.emptyList())
                .foundFiles(Collections.emptyList())
                .issues(issues)
                .build();
        }
        
        IPk2Driver driver = driverOpt.get();
        
        // Get all files in the PK2
        List<JMXFile> allFiles = driver.find(".*");
        foundFiles = allFiles.stream()
            .map(JMXFile::getName)
            .collect(Collectors.toList());
        
        // Check required files
        Set<String> requiredFiles = WellKnownFiles.getRequiredFiles(pk2Name);
        for (String requiredFile : requiredFiles) {
            boolean found = false;
            
            // Check if file is a regex pattern or contains wildcards
            boolean isRegexPattern = requiredFile.startsWith("(?i)") || 
                                    requiredFile.matches(".*[\\[\\]\\(\\)\\{\\}\\|\\+\\?\\^\\$].*");
            boolean hasWildcard = requiredFile.contains("*");
            
            if (isRegexPattern || hasWildcard) {
                try {
                    List<JMXFile> matches = driver.find(requiredFile);
                    if (matches.isEmpty()) {
                        // Try with .* prefix (fallback for partial matches like 'ifquestreward.txt' matches 'questreward.txt')
                        // Resolve pattern first to escape if needed, though requiredFile might be simple string
                        // But here we just prepend .* to the original string/pattern?
                        // If requiredFile was "*.nvm", driver handled conversion. 
                        // If we prepend .*, driver sees .*... which is regex.
                        // Ideally we try ".*" + requiredFile. But we need to handle special chars if it's not a regex.
                        // Pk2File.resolvePattern handles simple wildcards.
                        // If requiredFile is "questreward.txt", resolvePattern -> "questreward.txt".
                        // Prepend ".*" -> ".*questreward.txt".
                        // If requiredFile is "*.nvm", resolvePattern -> ".*\\.nvm". (Implicitly handled by driver).
                        // If we manually prepend ".*" to "*.nvm", we get ".*.*.nvm".
                        // Let's just try ".*" + requiredFile. If requiredFile has *, driver treats it as Regex if it has .*
                        // If we pass ".*" + "*.nvm" -> ".*" + "*.nvm"
                        // Pk2File.resolvePattern(".*" + "*.nvm")
                        // contains ".*" -> isRegex=true. Returns ".*" + "*.nvm".
                        // This regex ".*.*.nvm" matches "foo.nvm". But ".*" matches anything, "." matches any char.
                        // It works.
                        matches = driver.find(".*" + requiredFile);
                    }
                    found = !matches.isEmpty();
                } catch (Exception e) {
                    // Pattern invalid, skip pattern matching
                    log.debug("Invalid pattern for required file: {} - {}", requiredFile, e.getMessage());
                }
            } else {
                // No wildcards or regex, try exact match first, then try with .* prefix
                Optional<JMXFile> file = driver.findFirst(requiredFile);
                if (!file.isPresent()) {
                    // Try matching anywhere in the path
                    file = driver.findFirst(".*" + requiredFile.replace("\\", "\\\\"));
                }
                found = file.isPresent();
            }
            
            if (!found) {
                missingRequiredFiles.add(requiredFile);
                issues.add(Issue.builder()
                    .type(Issue.IssueType.MISSING_REQUIRED_FILE)
                    .pk2File(pk2Name)
                    .description("Missing required file: " + requiredFile)
                    .severity("CRITICAL")
                    .details("Required file '" + requiredFile + "' not found in " + pk2Name)
                    .build());
            }
        }
        
        // Check optional files
        Set<String> optionalFiles = WellKnownFiles.getOptionalFiles(pk2Name);
        for (String optionalFile : optionalFiles) {
            boolean found = false;
            
            // Check if file is a regex pattern or contains wildcards
            boolean isRegexPattern = optionalFile.startsWith("(?i)") || 
                                    optionalFile.matches(".*[\\[\\]\\(\\)\\{\\}\\|\\+\\?\\^\\$].*");
            boolean hasWildcard = optionalFile.contains("*");
            
            if (isRegexPattern || hasWildcard) {
                try {
                    List<JMXFile> matches = driver.find(optionalFile);
                    if (matches.isEmpty()) {
                         // Try with .* prefix fallback
                         matches = driver.find(".*" + optionalFile);
                    }
                    found = !matches.isEmpty();
                    if (!found) {
                        log.debug("Pattern '{}' found 0 matches in {}", optionalFile, pk2Name);
                    } else {
                        log.debug("Pattern '{}' found {} matches in {}", optionalFile, matches.size(), pk2Name);
                    }
                } catch (Exception e) {
                    // Pattern invalid, skip pattern matching
                    log.debug("Invalid pattern for optional file: {} - {}", optionalFile, e.getMessage());
                }
            } else {
                // No wildcards or regex, try exact match first, then try with .* prefix
                Optional<JMXFile> file = driver.findFirst(optionalFile);
                if (!file.isPresent()) {
                    // Try matching anywhere in the path
                    file = driver.findFirst(".*" + optionalFile.replace("\\", "\\\\"));
                }
                found = file.isPresent();
            }
            
            if (!found) {
                missingOptionalFiles.add(optionalFile);
                issues.add(Issue.builder()
                    .type(Issue.IssueType.MISSING_OPTIONAL_FILE)
                    .pk2File(pk2Name)
                    .description("Missing optional file: " + optionalFile)
                    .severity("WARNING")
                    .details("Optional file '" + optionalFile + "' not found in " + pk2Name)
                    .build());
            }
        }
        
        // Verify file readability
        for (JMXFile jmxFile : allFiles) {
            try (var is = jmxFile.getInputStream()) {
                if (is.read() == -1 && jmxFile.getSize() > 0) {
                    issues.add(Issue.builder()
                        .type(Issue.IssueType.FILE_CANNOT_BE_READ)
                        .pk2File(pk2Name)
                        .description("File cannot be read: " + jmxFile.getName())
                        .severity("WARNING")
                        .details("File '" + jmxFile.getName() + "' in " + pk2Name + " appears to be empty but has size " + jmxFile.getSize())
                        .build());
                }
            } catch (IOException e) {
                issues.add(Issue.builder()
                    .type(Issue.IssueType.FILE_CANNOT_BE_READ)
                    .pk2File(pk2Name)
                    .description("Error reading file: " + jmxFile.getName())
                    .severity("WARNING")
                    .details("Error reading '" + jmxFile.getName() + "' in " + pk2Name + ": " + e.getMessage())
                    .build());
            }
        }
        
        int requiredFilesFound = requiredFiles.size() - missingRequiredFiles.size();
        int optionalFilesFound = optionalFiles.size() - missingOptionalFiles.size();
        
        return Pk2FileReport.builder()
            .pk2FileName(pk2Name)
            .canBeOpened(true)
            .totalFiles(allFiles.size())
            .requiredFilesFound(requiredFilesFound)
            .requiredFilesMissing(missingRequiredFiles.size())
            .optionalFilesFound(optionalFilesFound)
            .missingRequiredFiles(missingRequiredFiles)
            .missingOptionalFiles(missingOptionalFiles)
            .foundFiles(foundFiles)
            .issues(issues)
            .build();
    }
    
    /**
     * Generate a text report.
     * 
     * @param report the compatibility report
     * @return text report as string
     */
    public static String generateTextReport(Pk2CompatibilityReport report) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println("=".repeat(80));
        pw.println("PK2 COMPATIBILITY REPORT");
        pw.println("=".repeat(80));
        pw.println();
        pw.println("Game Directory: " + report.getGameDirectory());
        pw.println("Timestamp: " + report.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        pw.println("Compatible: " + (report.isCompatible() ? "YES" : "NO"));
        pw.println("Total Issues: " + report.getTotalIssues());
        pw.println();
        
        // Summary by severity
        Map<String, List<Issue>> issuesBySeverity = report.getIssuesBySeverity();
        pw.println("Issues Summary:");
        pw.println("  CRITICAL: " + issuesBySeverity.getOrDefault("CRITICAL", Collections.emptyList()).size());
        pw.println("  WARNING:  " + issuesBySeverity.getOrDefault("WARNING", Collections.emptyList()).size());
        pw.println("  INFO:     " + issuesBySeverity.getOrDefault("INFO", Collections.emptyList()).size());
        pw.println();
        
        // PK2 File Reports
        pw.println("-".repeat(80));
        pw.println("PK2 FILE ANALYSIS");
        pw.println("-".repeat(80));
        pw.println();
        
        for (Pk2FileReport fileReport : report.getPk2FileReports().values()) {
            pw.println("PK2 File: " + fileReport.getPk2FileName());
            pw.println("  Can be opened: " + (fileReport.isCanBeOpened() ? "YES" : "NO"));
            if (fileReport.isCanBeOpened()) {
                pw.println("  Total files: " + fileReport.getTotalFiles());
                pw.println("  Required files found: " + fileReport.getRequiredFilesFound() + 
                          " / " + (fileReport.getRequiredFilesFound() + fileReport.getRequiredFilesMissing()));
                pw.println("  Optional files found: " + fileReport.getOptionalFilesFound());
                
                if (!fileReport.getMissingRequiredFiles().isEmpty()) {
                    pw.println("  Missing required files:");
                    for (String file : fileReport.getMissingRequiredFiles()) {
                        pw.println("    - " + file);
                    }
                }
                
                if (!fileReport.getMissingOptionalFiles().isEmpty()) {
                    pw.println("  Missing optional files:");
                    for (String file : fileReport.getMissingOptionalFiles()) {
                        pw.println("    - " + file);
                    }
                }
            }
            pw.println();
        }
        
        // Detailed Issues
        if (!report.getIssues().isEmpty()) {
            pw.println("-".repeat(80));
            pw.println("DETAILED ISSUES");
            pw.println("-".repeat(80));
            pw.println();
            
            // Group by severity
            List<Issue> criticalIssues = report.getCriticalIssues();
            if (!criticalIssues.isEmpty()) {
                pw.println("CRITICAL ISSUES:");
                for (Issue issue : criticalIssues) {
                    pw.println("  [" + issue.getType() + "] " + issue.getDescription());
                    pw.println("      PK2: " + issue.getPk2File());
                    if (issue.getDetails() != null) {
                        pw.println("      Details: " + issue.getDetails());
                    }
                    pw.println();
                }
            }
            
            List<Issue> warnings = report.getWarnings();
            if (!warnings.isEmpty()) {
                pw.println("WARNINGS:");
                for (Issue issue : warnings) {
                    pw.println("  [" + issue.getType() + "] " + issue.getDescription());
                    pw.println("      PK2: " + issue.getPk2File());
                    if (issue.getDetails() != null) {
                        pw.println("      Details: " + issue.getDetails());
                    }
                    pw.println();
                }
            }
        }
        
        pw.println("=".repeat(80));
        
        return sw.toString();
    }
    
    /**
     * Save report to a file.
     * 
     * @param report the compatibility report
     * @param outputPath path to save the report
     * @throws IOException if file cannot be written
     */
    public static void saveReport(Pk2CompatibilityReport report, String outputPath) throws IOException {
        String textReport = generateTextReport(report);
        Path path = Paths.get(outputPath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, textReport);
        log.info("Compatibility report saved to: {}", path.toAbsolutePath());
    }
    
    /**
     * Save report to default location (target/compatibility-reports/).
     * 
     * @param report the compatibility report
     * @return path where report was saved
     * @throws IOException if file cannot be written
     */
    public static Path saveReport(Pk2CompatibilityReport report) throws IOException {
        String dirName = Paths.get(report.getGameDirectory()).getFileName().toString();
        String timestamp = report.getTimestamp().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "compatibility-report_" + dirName + "_" + timestamp + ".txt";
        
        Path reportDir = Paths.get("target/compatibility-reports");
        Path reportPath = reportDir.resolve(filename);
        
        saveReport(report, reportPath.toString());
        return reportPath;
    }
}