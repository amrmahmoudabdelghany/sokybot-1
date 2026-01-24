package org.sokybot.pk2.test;

import lombok.extern.slf4j.Slf4j;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2.JMXFile;

import java.util.List;

/**
 * Temporary test to verify which files actually exist in the PK2 archives.
 */
@Slf4j
public class VerifyMissingFiles {
    
    public static void main(String[] args) {
        String gameDir = "/home/amr/sokybot-workspace/Cyper Online Official Client";
        
        try (Pk2TestFixture fixture = new Pk2TestFixture(gameDir)) {
            // Test Media.pk2 files
            IPk2Driver media = fixture.getMediaPk2();
            
            String[] patternsToTest = {
                // Exact matches
                "questreward.txt",
                "fullleveldata.txt",
                "reftext.txt",
                "GATEPORT.TXT",
                "gateport.txt",
                
                // Wildcard patterns
                "skilldata*.txt",
                ".*skilldata.*\\.txt",
                ".*skilldata.*\\.txt$",
                "skilldata.*\\.txt$",
                
                "portal*.txt",
                ".*portal.*\\.txt$",
                
                "npcdata*.txt",
                ".*npcdata.*\\.txt$",
                
                "teleport*.txt",
                ".*teleport.*\\.txt$",
                
                // Case-insensitive
                "(?i)gate.*port.*",
                "(?i).*gate.*port.*",
                
                // Data.pk2
            };
            
            log.info("=== Testing Media.pk2 patterns ===");
            for (String pattern : patternsToTest) {
                List<JMXFile> matches = media.find(pattern, 5);
                log.info("Pattern: '{}' -> Found: {} files", pattern, matches.size());
                if (!matches.isEmpty()) {
                    matches.forEach(f -> log.info("  - {}", f.getName()));
                }
            }
            
            // Test Data.pk2
            IPk2Driver data = fixture.getPk2("Data.pk2").orElse(null);
            if (data != null) {
                log.info("\n=== Testing Data.pk2 patterns ===");
                String[] dataPatterns = {
                    "*.nvm",
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
            
        } catch (Exception e) {
            log.error("Error verifying files", e);
        }
    }
}
