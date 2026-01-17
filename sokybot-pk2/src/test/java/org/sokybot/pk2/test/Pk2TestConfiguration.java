package org.sokybot.pk2.test;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Configuration system for PK2 testing framework.
 * Loads game directory path(s) from system properties, environment variables, and text files.
 * 
 * @author sokybot
 */
@Slf4j
public class Pk2TestConfiguration {
    
    private static final String PROP_GAME_PATH = "pk2.test.game.path";
    private static final String PROP_GAME_DIRECTORY = "pk2.test.game.directory";
    private static final String PROP_GAME_PATHS = "pk2.test.game.paths";
    private static final String PROP_GAME_PATHS_FILE = "pk2.test.game.paths.file";
    
    private static final String ENV_GAME_PATH = "PK2_TEST_GAME_PATH";
    private static final String ENV_GAME_DIRECTORY = "PK2_TEST_GAME_DIRECTORY";
    private static final String ENV_GAME_PATHS = "PK2_TEST_GAME_PATHS";
    private static final String ENV_GAME_PATHS_FILE = "PK2_TEST_GAME_PATHS_FILE";
    
    private static final String DEFAULT_PATHS_FILE = "pk2-test-game-paths.txt";
    
    private static volatile Pk2TestConfiguration instance;
    private final List<String> gameDirectories;
    
    private Pk2TestConfiguration() {
        this.gameDirectories = loadGameDirectories();
    }
    
    /**
     * Get singleton instance of configuration.
     */
    public static Pk2TestConfiguration getInstance() {
        if (instance == null) {
            synchronized (Pk2TestConfiguration.class) {
                if (instance == null) {
                    instance = new Pk2TestConfiguration();
                }
            }
        }
        return instance;
    }
    
    /**
     * Get the first configured game directory (for single directory tests).
     * 
     * @return game directory path
     * @throws Pk2TestConfigurationException if no directory is configured
     */
    public String getGameDirectory() {
        if (gameDirectories.isEmpty()) {
            throw new Pk2TestConfigurationException(
                "No game directory configured. Set system property '" + PROP_GAME_PATH + 
                "', environment variable '" + ENV_GAME_PATH + 
                "', or create '" + DEFAULT_PATHS_FILE + "' in test resources."
            );
        }
        return gameDirectories.get(0);
    }
    
    /**
     * Get all configured game directories (for parameterized tests).
     * 
     * @return list of game directory paths
     */
    public List<String> getGameDirectories() {
        return Collections.unmodifiableList(gameDirectories);
    }
    
    /**
     * Check if any game directory is configured.
     */
    public boolean isConfigured() {
        return !gameDirectories.isEmpty();
    }
    
    /**
     * Load game directories from various sources (in order of precedence).
     */
    private List<String> loadGameDirectories() {
        // 1. Check system properties for single directory
        String singlePath = System.getProperty(PROP_GAME_PATH);
        if (singlePath == null) {
            singlePath = System.getProperty(PROP_GAME_DIRECTORY);
        }
        if (singlePath != null && !singlePath.isBlank()) {
            List<String> paths = Arrays.asList(singlePath.trim());
            log.info("Loaded game directory from system property: {}", singlePath);
            return validateDirectories(paths);
        }
        
        // 2. Check environment variables for single directory
        singlePath = System.getenv(ENV_GAME_PATH);
        if (singlePath == null) {
            singlePath = System.getenv(ENV_GAME_DIRECTORY);
        }
        if (singlePath != null && !singlePath.isBlank()) {
            List<String> paths = Arrays.asList(singlePath.trim());
            log.info("Loaded game directory from environment variable: {}", singlePath);
            return validateDirectories(paths);
        }
        
        // 3. Check system property for multiple directories (comma-separated)
        String pathsProperty = System.getProperty(PROP_GAME_PATHS);
        if (pathsProperty != null && !pathsProperty.isBlank()) {
            List<String> paths = Arrays.stream(pathsProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
            if (!paths.isEmpty()) {
                log.info("Loaded {} game directories from system property", paths.size());
                return validateDirectories(paths);
            }
        }
        
        // 4. Check environment variable for multiple directories (comma-separated)
        String pathsEnv = System.getenv(ENV_GAME_PATHS);
        if (pathsEnv != null && !pathsEnv.isBlank()) {
            List<String> paths = Arrays.stream(pathsEnv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
            if (!paths.isEmpty()) {
                log.info("Loaded {} game directories from environment variable", paths.size());
                return validateDirectories(paths);
            }
        }
        
        // 5. Check for text file (custom path first)
        String filePath = System.getProperty(PROP_GAME_PATHS_FILE);
        if (filePath == null) {
            filePath = System.getenv(ENV_GAME_PATHS_FILE);
        }
        
        List<String> pathsFromFile = loadFromFile(filePath);
        if (!pathsFromFile.isEmpty()) {
            log.info("Loaded {} game directories from file: {}", pathsFromFile.size(), 
                filePath != null ? filePath : DEFAULT_PATHS_FILE);
            return validateDirectories(pathsFromFile);
        }
        
        // 6. Try default file location
        pathsFromFile = loadFromFile(DEFAULT_PATHS_FILE);
        if (!pathsFromFile.isEmpty()) {
            log.info("Loaded {} game directories from default file: {}", 
                pathsFromFile.size(), DEFAULT_PATHS_FILE);
            return validateDirectories(pathsFromFile);
        }
        
        log.warn("No game directories configured. Tests requiring game directories will fail.");
        return Collections.emptyList();
    }
    
    /**
     * Load game directories from a text file.
     * 
     * @param filePath path to the text file (can be relative or absolute, null for default)
     * @return list of game directory paths
     */
    public List<String> loadFromFile(String filePath) {
        Path path;
        
        if (filePath == null || filePath.isBlank()) {
            // Try to load from test resources
            try {
                java.net.URL resource = getClass().getClassLoader()
                    .getResource(DEFAULT_PATHS_FILE);
                if (resource == null) {
                    return Collections.emptyList();
                }
                path = Paths.get(resource.toURI());
            } catch (Exception e) {
                log.debug("Could not load default paths file from resources: {}", e.getMessage());
                return Collections.emptyList();
            }
        } else {
            path = Paths.get(filePath);
            if (!path.isAbsolute()) {
                // Try relative to project root
                Path projectRoot = Paths.get(System.getProperty("user.dir"));
                path = projectRoot.resolve(filePath);
            }
        }
        
        if (!Files.exists(path)) {
            log.debug("Paths file does not exist: {}", path);
            return Collections.emptyList();
        }
        
        try {
            return Files.lines(path)
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error reading paths file: {}", path, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Validate that directories exist and contain required files.
     */
    private List<String> validateDirectories(List<String> directories) {
        return directories.stream()
            .filter(this::isValidGameDirectory)
            .collect(Collectors.toList());
    }
    
    /**
     * Check if a directory is a valid game directory.
     */
    private boolean isValidGameDirectory(String directoryPath) {
        try {
            Path dir = Paths.get(directoryPath);
            if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                log.warn("Game directory does not exist or is not a directory: {}", directoryPath);
                return false;
            }
            
            // Check for Media.pk2 (required)
            Path mediaPk2 = dir.resolve("Media.pk2");
            if (!Files.exists(mediaPk2)) {
                log.warn("Game directory does not contain Media.pk2: {}", directoryPath);
                return false;
            }
            
            log.debug("Validated game directory: {}", directoryPath);
            return true;
        } catch (Exception e) {
            log.warn("Error validating game directory: {}", directoryPath, e);
            return false;
        }
    }
    
    /**
     * Exception thrown when configuration is invalid or missing.
     */
    public static class Pk2TestConfigurationException extends RuntimeException {
        public Pk2TestConfigurationException(String message) {
            super(message);
        }
        
        public Pk2TestConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}