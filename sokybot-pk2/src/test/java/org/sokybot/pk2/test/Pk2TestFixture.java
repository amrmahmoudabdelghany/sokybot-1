package org.sokybot.pk2.test;

import lombok.extern.slf4j.Slf4j;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2.JMXFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Test fixture for accessing PK2 files in a game directory.
 * Provides convenient methods to access Media.pk2, Data.pk2, and other PK2 files.
 * 
 * @author sokybot
 */
@Slf4j
public class Pk2TestFixture implements AutoCloseable {
    
    private final String gameDirectory;
    private final Map<String, IPk2Driver> pk2Drivers = new ConcurrentHashMap<>();
    private final Set<String> availablePk2Files = new HashSet<>();
    
    // Extracted directory mapping: PK2 file name -> extracted directory path
    private final Map<String, Path> extractedDirectories = new HashMap<>();
    private static final Map<String, String> PK2_TO_DIRECTORY_MAPPING = Map.of(
        "Media.pk2", "Media",
        "Data.pk2", "Data",
        "Map.pk2", "Map",
        "Music.pk2", "Music",
        "Particles.pk2", "Particles"
    );
    
    /**
     * Create a fixture using the configured game directory.
     * 
     * @throws Pk2TestConfiguration.Pk2TestConfigurationException if no directory is configured
     */
    public Pk2TestFixture() {
        this(Pk2TestConfiguration.getInstance().getGameDirectory());
    }
    
    /**
     * Create a fixture for a specific game directory.
     * 
     * @param gameDirectory path to the game directory
     */
    public Pk2TestFixture(String gameDirectory) {
        this.gameDirectory = Paths.get(gameDirectory).normalize().toString();
        discoverPk2Files();
    }
    
    /**
     * Get the game directory path.
     */
    public String getGameDirectory() {
        return gameDirectory;
    }
    
    /**
     * Get Media.pk2 driver (required file).
     * 
     * @return IPk2Driver for Media.pk2
     * @throws IllegalStateException if Media.pk2 does not exist
     */
    public IPk2Driver getMediaPk2() {
        return getPk2("Media.pk2")
            .orElseThrow(() -> new IllegalStateException(
                "Media.pk2 not found in game directory: " + gameDirectory));
    }
    
    /**
     * Get Data.pk2 driver (optional).
     * 
     * @return Optional containing IPk2Driver for Data.pk2, or empty if not found
     */
    public Optional<IPk2Driver> getDataPk2() {
        return getPk2("Data.pk2");
    }
    
    /**
     * Get Map.pk2 driver (optional).
     * 
     * @return Optional containing IPk2Driver for Map.pk2, or empty if not found
     */
    public Optional<IPk2Driver> getMapPk2() {
        return getPk2("Map.pk2");
    }
    
    /**
     * Get Music.pk2 driver (optional).
     * 
     * @return Optional containing IPk2Driver for Music.pk2, or empty if not found
     */
    public Optional<IPk2Driver> getMusicPk2() {
        return getPk2("Music.pk2");
    }
    
    /**
     * Get Particles.pk2 driver (optional).
     * 
     * @return Optional containing IPk2Driver for Particles.pk2, or empty if not found
     */
    public Optional<IPk2Driver> getParticlesPk2() {
        return getPk2("Particles.pk2");
    }
    
    /**
     * Get a PK2 file driver by name.
     * 
     * @param fileName name of the PK2 file (e.g., "Media.pk2", "Map.pk2")
     * @return Optional containing IPk2Driver, or empty if file does not exist
     */
    public Optional<IPk2Driver> getPk2(String fileName) {
        if (!availablePk2Files.contains(fileName)) {
            return Optional.empty();
        }
        
        return Optional.of(pk2Drivers.computeIfAbsent(fileName, this::openPk2File));
    }
    
    /**
     * Get all discovered PK2 files.
     * 
     * @return map of PK2 file names to their drivers
     */
    public Map<String, IPk2Driver> getAllPk2Files() {
        Map<String, IPk2Driver> result = new HashMap<>();
        for (String fileName : availablePk2Files) {
            getPk2(fileName).ifPresent(driver -> result.put(fileName, driver));
        }
        return result;
    }
    
    /**
     * Get list of available PK2 file names.
     * 
     * @return set of PK2 file names that exist in the game directory
     */
    public Set<String> getAvailablePk2Files() {
        return Collections.unmodifiableSet(availablePk2Files);
    }
    
    /**
     * Find a file in a specific PK2 archive.
     * 
     * @param pk2Name name of the PK2 file (e.g., "Media.pk2")
     * @param filePath path to the file within the PK2 (can be regex or wildcard pattern)
     * @return Optional containing the JMXFile, or empty if not found
     */
    public Optional<JMXFile> findFile(String pk2Name, String filePath) {
        // Convert wildcard pattern to regex if needed
        final String searchPattern;
        // Wildcard conversion is now handled by the driver
        searchPattern = filePath;
        
        return getPk2(pk2Name)
            .flatMap(driver -> driver.findFirst(searchPattern));
    }
    
    /**
     * Find multiple files in a specific PK2 archive.
     * 
     * @param pk2Name name of the PK2 file
     * @param regex regex pattern or wildcard pattern to match file paths
     * @return list of matching JMXFiles
     */
    public List<JMXFile> findFiles(String pk2Name, String regex) {
        // Convert wildcard pattern to regex if needed
        final String searchPattern;
        // Wildcard conversion is now handled by the driver
        searchPattern = regex;
        
        return getPk2(pk2Name)
            .map(driver -> driver.find(searchPattern))
            .orElse(Collections.emptyList());
    }
    
    /**
     * Check if a well-known file exists in a PK2 archive.
     * Handles both exact file paths and wildcard patterns.
     * 
     * @param pk2Name name of the PK2 file
     * @param filePath path to the file within the PK2 (can be pattern like "*.nvm")
     * @return true if the file exists
     */
    public boolean isWellKnownFilePresent(String pk2Name, String filePath) {
        // findFile() now handles wildcard patterns, so we can use it directly
        Optional<JMXFile> match = findFile(pk2Name, filePath);
        if (match.isPresent()) {
            return true;
        }
        
        // If it's a wildcard pattern, also try findFiles() to get all matches
        if (filePath.contains("*")) {
            List<JMXFile> matches = findFiles(pk2Name, filePath);
            return !matches.isEmpty();
        }
        
        return false;
    }
    
    /**
     * Get the path to the extracted directory for a PK2 file, if it exists.
     * 
     * @param pk2Name name of the PK2 file (e.g., "Media.pk2")
     * @return Optional containing the path to extracted directory, or empty if not found
     */
    public Optional<Path> getExtractedDirectory(String pk2Name) {
        return Optional.ofNullable(extractedDirectories.get(pk2Name));
    }
    
    /**
     * Check if an extracted directory exists for a PK2 file.
     * 
     * @param pk2Name name of the PK2 file
     * @return true if extracted directory exists
     */
    public boolean hasExtractedDirectory(String pk2Name) {
        return extractedDirectories.containsKey(pk2Name);
    }
    
    /**
     * Get all extracted directories that were discovered.
     * 
     * @return map of PK2 file names to their extracted directory paths
     */
    public Map<String, Path> getExtractedDirectories() {
        return Collections.unmodifiableMap(extractedDirectories);
    }
    
    /**
     * Discover all .pk2 files in the game directory and extracted directories.
     */
    private void discoverPk2Files() {
        try {
            Path gameDir = Paths.get(gameDirectory);
            if (!Files.exists(gameDir) || !Files.isDirectory(gameDir)) {
                throw new IllegalArgumentException(
                    "Game directory does not exist or is not a directory: " + gameDirectory);
            }
            
            // Discover PK2 files
            try (Stream<Path> files = Files.list(gameDir)) {
                files.filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.toLowerCase().endsWith(".pk2"))
                    .forEach(availablePk2Files::add);
            }
            
            // Discover extracted directories
            discoverExtractedDirectories();
            
            log.debug("Discovered {} PK2 files and {} extracted directories in directory: {}", 
                availablePk2Files.size(), extractedDirectories.size(), gameDirectory);
            
        } catch (IOException e) {
            throw new RuntimeException("Error discovering PK2 files in: " + gameDirectory, e);
        }
    }
    
    /**
     * Discover extracted directories that correspond to PK2 files.
     */
    private void discoverExtractedDirectories() {
        Path gameDir = Paths.get(gameDirectory);
        
        PK2_TO_DIRECTORY_MAPPING.forEach((pk2Name, dirName) -> {
            Path extractedDir = gameDir.resolve(dirName);
            if (Files.exists(extractedDir) && Files.isDirectory(extractedDir)) {
                extractedDirectories.put(pk2Name, extractedDir);
                log.debug("Found extracted directory for {}: {}", pk2Name, extractedDir);
            }
        });
    }
    
    /**
     * Open a PK2 file and return its driver.
     */
    private IPk2Driver openPk2File(String fileName) {
        Path pk2Path = Paths.get(gameDirectory, fileName);
        String absolutePath = pk2Path.toAbsolutePath().toString();
        
        log.debug("Opening PK2 file: {}", absolutePath);
        try {
            return IPk2Driver.open(absolutePath);
        } catch (Exception e) {
            log.error("Failed to open PK2 file: {}", absolutePath, e);
            throw new RuntimeException("Failed to open PK2 file: " + fileName, e);
        }
    }
    
    @Override
    public void close() throws Exception {
        List<Exception> errors = new ArrayList<>();
        
        for (Map.Entry<String, IPk2Driver> entry : pk2Drivers.entrySet()) {
            try {
                entry.getValue().close();
                log.debug("Closed PK2 file: {}", entry.getKey());
            } catch (Exception e) {
                log.warn("Error closing PK2 file: {}", entry.getKey(), e);
                errors.add(e);
            }
        }
        
        pk2Drivers.clear();
        
        if (!errors.isEmpty()) {
            IOException exception = new IOException("Errors occurred while closing PK2 files");
            errors.forEach(exception::addSuppressed);
            throw exception;
        }
    }
}