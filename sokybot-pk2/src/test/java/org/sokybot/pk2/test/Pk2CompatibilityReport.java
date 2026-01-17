package org.sokybot.pk2.test;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Report model for PK2 compatibility testing.
 * Collects all issues found during compatibility testing.
 * 
 * @author sokybot
 */
@Getter
@Builder
@ToString
public class Pk2CompatibilityReport {
    
    private final String gameDirectory;
    private final LocalDateTime timestamp;
    private final boolean compatible;
    private final List<Issue> issues;
    private final Map<String, Pk2FileReport> pk2FileReports;
    
    /**
     * Issue found during compatibility testing.
     */
    @Getter
    @Builder
    @ToString
    public static class Issue {
        private final IssueType type;
        private final String pk2File;
        private final String description;
        private final String severity; // CRITICAL, WARNING, INFO
        private final String details;
        
        public enum IssueType {
            MISSING_REQUIRED_FILE,
            MISSING_OPTIONAL_FILE,
            PK2_CANNOT_BE_OPENED,
            FILE_CANNOT_BE_READ,
            FILE_SIZE_MISMATCH,
            UNEXPECTED_ERROR,
            COMPATIBILITY_ISSUE
        }
    }
    
    /**
     * Report for a specific PK2 file.
     */
    @Getter
    @Builder
    @ToString
    public static class Pk2FileReport {
        private final String pk2FileName;
        private final boolean canBeOpened;
        private final int totalFiles;
        private final int requiredFilesFound;
        private final int requiredFilesMissing;
        private final int optionalFilesFound;
        private final List<String> missingRequiredFiles;
        private final List<String> missingOptionalFiles;
        private final List<String> foundFiles;
        private final List<Issue> issues;
    }
    
    /**
     * Get total number of issues.
     */
    public int getTotalIssues() {
        return issues != null ? issues.size() : 0;
    }
    
    /**
     * Get issues by severity.
     */
    public Map<String, List<Issue>> getIssuesBySeverity() {
        Map<String, List<Issue>> result = new HashMap<>();
        if (issues != null) {
            for (Issue issue : issues) {
                result.computeIfAbsent(issue.getSeverity(), k -> new ArrayList<>()).add(issue);
            }
        }
        return result;
    }
    
    /**
     * Get critical issues.
     */
    public List<Issue> getCriticalIssues() {
        return getIssuesBySeverity().getOrDefault("CRITICAL", Collections.emptyList());
    }
    
    /**
     * Get warnings.
     */
    public List<Issue> getWarnings() {
        return getIssuesBySeverity().getOrDefault("WARNING", Collections.emptyList());
    }
    
    /**
     * Check if there are any critical issues.
     */
    public boolean hasCriticalIssues() {
        return !getCriticalIssues().isEmpty();
    }
}