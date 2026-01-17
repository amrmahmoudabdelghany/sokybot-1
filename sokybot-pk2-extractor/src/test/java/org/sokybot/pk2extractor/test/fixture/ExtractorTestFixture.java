package org.sokybot.pk2extractor.test.fixture;

import lombok.extern.slf4j.Slf4j;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.pk2.JMXFile;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Test fixture for PK2 extractor tests.
 * Provides access to PK2 files for testing.
 * 
 * <p>This fixture wraps the PK2 test fixture and provides additional
 * utilities specific to extractor testing.
 * 
 * @author sokybot
 */
@Slf4j
public class ExtractorTestFixture implements Closeable {

    private final Pk2TestFixtureWrapper pk2Fixture;
    private final ExtractorTestConfiguration config;

    public ExtractorTestFixture(ExtractorTestConfiguration config) {
        this.config = config;
        this.pk2Fixture = new Pk2TestFixtureWrapper(config);
    }

    /**
     * Get Media.pk2 driver.
     * 
     * @return Media.pk2 driver
     */
    public IPk2Driver getMediaPk2() {
        return pk2Fixture.getMediaPk2();
    }

    /**
     * Get Data.pk2 driver if available.
     * 
     * @return Optional Data.pk2 driver
     */
    public Optional<IPk2Driver> getDataPk2() {
        return pk2Fixture.getDataPk2();
    }

    /**
     * Get a specific PK2 driver by name.
     * 
     * @param pk2Name Name of the PK2 file (e.g., "Media.pk2", "Data.pk2")
     * @return Optional PK2 driver
     */
    public Optional<IPk2Driver> getPk2(String pk2Name) {
        return pk2Fixture.getPk2(pk2Name);
    }

    /**
     * Find a file in a specific PK2 archive.
     * 
     * @param pk2Name Name of the PK2 file
     * @param fileName Name or pattern of the file to find
     * @return Optional JMX file
     */
    public Optional<JMXFile> findFile(String pk2Name, String fileName) {
        return pk2Fixture.findFile(pk2Name, fileName);
    }

    /**
     * Get all available PK2 files.
     * 
     * @return Set of PK2 file names
     */
    public Set<String> getAvailablePk2Files() {
        return pk2Fixture.getAvailablePk2Files();
    }

    /**
     * Get the game directory path.
     * 
     * @return Game directory path
     */
    public Path getGameDirectory() {
        return pk2Fixture.getGameDirectory();
    }

    /**
     * Check if fixture is valid and ready to use.
     * 
     * @return true if fixture is valid
     */
    public boolean isValid() {
        return pk2Fixture.isValid();
    }

    @Override
    public void close() {
        if (pk2Fixture != null) {
            pk2Fixture.close();
        }
    }

    /**
     * Wrapper around the PK2 test fixture.
     * This is a simplified wrapper that delegates to the actual PK2 test fixture.
     * If the PK2 test fixture classes exist, they will be used. Otherwise,
     * this provides a minimal implementation for basic testing.
     */
    private static class Pk2TestFixtureWrapper implements Closeable {
        private final ExtractorTestConfiguration config;
        private IPk2Driver mediaPk2;
        private Map<String, IPk2Driver> pk2Drivers;

        public Pk2TestFixtureWrapper(ExtractorTestConfiguration config) {
            this.config = config;
            initialize();
        }

        private void initialize() {
            try {
                Path gameDir = config.getGameDirectory();
                if (gameDir == null || !gameDir.toFile().exists()) {
                    log.warn("Game directory not found: {}", gameDir);
                    return;
                }

                // Try to open Media.pk2
                Path mediaPk2Path = gameDir.resolve("Media.pk2");
                if (mediaPk2Path.toFile().exists()) {
                    mediaPk2 = IPk2Driver.open(mediaPk2Path.toString());
                    log.info("Opened Media.pk2: {}", mediaPk2Path);
                } else {
                    log.warn("Media.pk2 not found at: {}", mediaPk2Path);
                }

                // Try to open Data.pk2 (optional)
                Path dataPk2Path = gameDir.resolve("Data.pk2");
                if (dataPk2Path.toFile().exists()) {
                    try {
                        IPk2Driver dataPk2 = IPk2Driver.open(dataPk2Path.toString());
                        if (pk2Drivers == null) {
                            pk2Drivers = new java.util.HashMap<>();
                        }
                        pk2Drivers.put("Data.pk2", dataPk2);
                        log.info("Opened Data.pk2: {}", dataPk2Path);
                    } catch (Exception e) {
                        log.warn("Failed to open Data.pk2: {}", e.getMessage());
                    }
                }

            } catch (Exception e) {
                log.error("Failed to initialize PK2 fixture", e);
            }
        }

        public IPk2Driver getMediaPk2() {
            return mediaPk2;
        }

        public Optional<IPk2Driver> getDataPk2() {
            if (pk2Drivers != null) {
                return Optional.ofNullable(pk2Drivers.get("Data.pk2"));
            }
            return Optional.empty();
        }

        public Optional<IPk2Driver> getPk2(String pk2Name) {
            if ("Media.pk2".equalsIgnoreCase(pk2Name)) {
                return Optional.ofNullable(mediaPk2);
            }
            if (pk2Drivers != null) {
                return Optional.ofNullable(pk2Drivers.get(pk2Name));
            }
            return Optional.empty();
        }

        public Optional<JMXFile> findFile(String pk2Name, String fileName) {
            Optional<IPk2Driver> driver = getPk2(pk2Name);
            if (driver.isPresent()) {
                return driver.get().findFirst(fileName);
            }
            return Optional.empty();
        }

        public Set<String> getAvailablePk2Files() {
            java.util.Set<String> files = new java.util.HashSet<>();
            if (mediaPk2 != null) {
                files.add("Media.pk2");
            }
            if (pk2Drivers != null) {
                files.addAll(pk2Drivers.keySet());
            }
            return files;
        }

        public Path getGameDirectory() {
            return config.getGameDirectory();
        }

        public boolean isValid() {
            return mediaPk2 != null;
        }

        @Override
        public void close() {
            if (mediaPk2 != null) {
                try {
                    mediaPk2.close();
                } catch (Exception e) {
                    log.warn("Error closing Media.pk2", e);
                }
            }
            if (pk2Drivers != null) {
                for (IPk2Driver driver : pk2Drivers.values()) {
                    try {
                        driver.close();
                    } catch (Exception e) {
                        log.warn("Error closing PK2 driver", e);
                    }
                }
                pk2Drivers.clear();
            }
        }
    }
}
