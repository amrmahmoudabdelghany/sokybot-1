package org.sokybot.pk2.test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Defines well-known files that should exist in PK2 archives.
 * Used for compatibility testing and file existence verification.
 * 
 * @author sokybot
 */
public class WellKnownFiles {
    
    // Media.pk2 required files
    private static final Set<String> MEDIA_PK2_REQUIRED = Set.of(
        "SV.T",
        "divisioninfo.txt",
        "type.txt",
        "characterdata.txt",
        "itemdata.txt"
    );
    
    // Media.pk2 optional but common files
    private static final Set<String> MEDIA_PK2_OPTIONAL = Set.of(
        "(?i)gate.*port.*",  // Case-insensitive: matches gateport.txt, GATEWAYPORT, GATEPORT.TXT, etc.
        "skilldata*.txt",
        "regioninfo.txt",
        "teleport*.txt",
        "portal*.txt",
        "npcdata*.txt",
        "reftext.txt",
        "refpackageitem.txt",
        "magicoption.txt",
        "teleportlink.txt",
        "questdata.txt",
        "questreward.txt",
        "fullleveldata.txt",
        "fileinfo.txt",
        "md.txt",
        "srk.txt"
    );
    
    // Data.pk2 common files
    private static final Set<String> DATA_PK2_OPTIONAL = Set.of(
        "*.nvm",
        "*.bsr",
        "*.cpd",
        "*.rd"
    );
    
    // Map.pk2 common files (structure varies)
    private static final Set<String> MAP_PK2_OPTIONAL = Set.of(
        "*.m",
        "*.o",
        "*.o2",
        "*.t"
    );
    
    // Music.pk2 common files (structure varies)
    private static final Set<String> MUSIC_PK2_OPTIONAL = Collections.emptySet();
    
    // Particles.pk2 common files (structure varies)
    private static final Set<String> PARTICLES_PK2_OPTIONAL = Collections.emptySet();
    
    /**
     * Get required files for a PK2 archive.
     * 
     * @param pk2Name name of the PK2 file (e.g., "Media.pk2")
     * @return set of required file paths (regex patterns or exact names)
     */
    public static Set<String> getRequiredFiles(String pk2Name) {
        if (pk2Name.equalsIgnoreCase("Media.pk2")) {
            return new HashSet<>(MEDIA_PK2_REQUIRED);
        }
        return Collections.emptySet();
    }
    
    /**
     * Get optional but common files for a PK2 archive.
     * 
     * @param pk2Name name of the PK2 file (e.g., "Media.pk2")
     * @return set of optional file paths (regex patterns or exact names)
     */
    public static Set<String> getOptionalFiles(String pk2Name) {
        String name = pk2Name.toLowerCase();
        if (name.equals("media.pk2")) {
            return new HashSet<>(MEDIA_PK2_OPTIONAL);
        } else if (name.equals("data.pk2")) {
            return new HashSet<>(DATA_PK2_OPTIONAL);
        } else if (name.equals("map.pk2")) {
            return new HashSet<>(MAP_PK2_OPTIONAL);
        } else if (name.equals("music.pk2")) {
            return new HashSet<>(MUSIC_PK2_OPTIONAL);
        } else if (name.equals("particles.pk2")) {
            return new HashSet<>(PARTICLES_PK2_OPTIONAL);
        }
        return Collections.emptySet();
    }
    
    /**
     * Get all files (required + optional) for a PK2 archive.
     * 
     * @param pk2Name name of the PK2 file
     * @return set of all well-known file paths
     */
    public static Set<String> getAllFiles(String pk2Name) {
        Set<String> all = new HashSet<>();
        all.addAll(getRequiredFiles(pk2Name));
        all.addAll(getOptionalFiles(pk2Name));
        return all;
    }
    
    /**
     * Check if a file is required for a PK2 archive.
     * 
     * @param pk2Name name of the PK2 file
     * @param filePath path to the file within the PK2
     * @return true if the file is required
     */
    public static boolean isRequired(String pk2Name, String filePath) {
        Set<String> required = getRequiredFiles(pk2Name);
        return required.contains(filePath) || 
               required.stream().anyMatch(pattern -> matchesPattern(filePath, pattern));
    }
    
    /**
     * Check if a file is well-known (required or optional) for a PK2 archive.
     * 
     * @param pk2Name name of the PK2 file
     * @param filePath path to the file within the PK2
     * @return true if the file is well-known
     */
    public static boolean isWellKnown(String pk2Name, String filePath) {
        Set<String> all = getAllFiles(pk2Name);
        return all.contains(filePath) || 
               all.stream().anyMatch(pattern -> matchesPattern(filePath, pattern));
    }
    
    /**
     * Check if a file path matches a pattern (supports * wildcards and case-insensitive patterns).
     */
    private static boolean matchesPattern(String filePath, String pattern) {
        if (pattern.equals(filePath)) {
            return true;
        }
        
        // If pattern is already a regex (starts with (?i) or contains regex special chars), use as-is
        if (pattern.startsWith("(?i)") || pattern.matches(".*[\\[\\]\\(\\)\\{\\}\\|\\+\\?\\^\\$].*")) {
            return filePath.matches(pattern);
        }
        
        // Convert simple wildcard pattern to regex
        // Escape special regex characters, then replace * wildcard
        String regex = pattern
            .replace("\\", "\\\\")
            .replace(".", "\\.")
            .replace("+", "\\+")
            .replace("?", "\\?")
            .replace("^", "\\^")
            .replace("$", "\\$")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("|", "\\|")
            .replace("*", ".*"); // Replace * with .* after escaping
        
        return filePath.matches(regex);
    }
    
    /**
     * Get list of PK2 files that have required files defined.
     */
    public static List<String> getPk2FilesWithRequirements() {
        return Arrays.asList("Media.pk2");
    }
}