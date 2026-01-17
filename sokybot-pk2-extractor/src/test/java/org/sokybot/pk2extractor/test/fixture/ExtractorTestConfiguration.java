package org.sokybot.pk2extractor.test.fixture;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration for extractor tests.
 * 
 * <p>Configuration can be provided via:
 * <ul>
 *   <li>System property: <code>sokybot.test.game.directory</code></li>
 *   <li>Environment variable: <code>SOKYBOT_TEST_GAME_DIRECTORY</code></li>
 *   <li>Properties file: <code>extractor-test.properties</code> in user home or test resources</li>
 * </ul>
 * 
 * @author sokybot
 */
@Slf4j
public class ExtractorTestConfiguration {

    private static final String PROPERTY_GAME_DIRECTORY = "sokybot.test.game.directory";
    private static final String ENV_GAME_DIRECTORY = "SOKYBOT_TEST_GAME_DIRECTORY";
    private static final String PROPERTIES_FILE = "extractor-test.properties";
    private static final String PROPERTIES_KEY_GAME_DIRECTORY = "game.directory";

    private static ExtractorTestConfiguration instance;
    
    private final Path gameDirectory;
    private final boolean configured;

    private ExtractorTestConfiguration() {
        Path dir = findGameDirectory();
        this.gameDirectory = dir;
        this.configured = dir != null && Files.exists(dir) && Files.isDirectory(dir);
        
        if (configured) {
            log.info("Extractor test configuration loaded. Game directory: {}", dir);
        } else {
            log.debug("Extractor test configuration not found. Compatibility tests will be skipped.");
        }
    }

    /**
     * Get the singleton instance.
     * 
     * @return Configuration instance
     */
    public static synchronized ExtractorTestConfiguration getInstance() {
        if (instance == null) {
            instance = new ExtractorTestConfiguration();
        }
        return instance;
    }

    /**
     * Find the game directory using various sources.
     * 
     * @return Game directory path, or null if not found
     */
    private Path findGameDirectory() {
        // 1. Check system property
        String sysProp = System.getProperty(PROPERTY_GAME_DIRECTORY);
        if (sysProp != null && !sysProp.isEmpty()) {
            Path path = Paths.get(sysProp);
            if (Files.exists(path)) {
                return path;
            }
            log.warn("System property {} points to non-existent directory: {}", PROPERTY_GAME_DIRECTORY, sysProp);
        }

        // 2. Check environment variable
        String envVar = System.getenv(ENV_GAME_DIRECTORY);
        if (envVar != null && !envVar.isEmpty()) {
            Path path = Paths.get(envVar);
            if (Files.exists(path)) {
                return path;
            }
            log.warn("Environment variable {} points to non-existent directory: {}", ENV_GAME_DIRECTORY, envVar);
        }

        // 3. Check properties file in user home
        Path homeProps = Paths.get(System.getProperty("user.home"), PROPERTIES_FILE);
        Path dir = loadGameDirectoryFromProperties(homeProps);
        if (dir != null) {
            return dir;
        }

        // 4. Check properties file in test resources (if available)
        try {
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE);
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String dirStr = props.getProperty(PROPERTIES_KEY_GAME_DIRECTORY);
                if (dirStr != null && !dirStr.isEmpty()) {
                    Path path = Paths.get(dirStr);
                    if (Files.exists(path)) {
                        return path;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not load properties from classpath: {}", e.getMessage());
        }

        // 5. Check pk2-test-game-paths.txt in test resources (classpath)
        try {
            java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("pk2-test-game-paths.txt");
            if (is != null) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            Path path = Paths.get(line);
                            if (Files.exists(path)) {
                                return path;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not load pk2-test-game-paths.txt from classpath: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Load game directory from properties file.
     * 
     * @param propsFile Properties file path
     * @return Game directory path, or null if not found
     */
    private Path loadGameDirectoryFromProperties(Path propsFile) {
        if (!Files.exists(propsFile)) {
            return null;
        }

        try {
            Properties props = new Properties();
            try (java.io.InputStream is = Files.newInputStream(propsFile)) {
                props.load(is);
            }
            String dirStr = props.getProperty(PROPERTIES_KEY_GAME_DIRECTORY);
            if (dirStr != null && !dirStr.isEmpty()) {
                Path path = Paths.get(dirStr);
                if (Files.exists(path)) {
                    return path;
                }
            }
        } catch (IOException e) {
            log.debug("Could not load properties from {}: {}", propsFile, e.getMessage());
        }

        return null;
    }

    /**
     * Check if configuration is available.
     * 
     * @return true if game directory is configured and exists
     */
    public boolean isConfigured() {
        return configured;
    }

    /**
     * Get the game directory path.
     * 
     * @return Game directory path, or null if not configured
     */
    public Path getGameDirectory() {
        return gameDirectory;
    }
}
