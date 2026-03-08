package org.sokybot.commons.script;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for file-system watchers that monitor a directory for script
 * changes. Handles WatchService lifecycle, directory creation, debounced
 * file-change notification, and the background polling thread.
 *
 * Subclasses implement {@link #onFileChanged(Path)} and
 * {@link #onFileRemoved(Path)} to react to filesystem events.
 */
public abstract class AbstractScriptWatcher {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final String[] fileExtensions;
    private ExecutorService watchExecutor;
    private WatchService watchService;
    private Path watchDirectory;

    /**
     * @param fileExtensions accepted extensions including the dot, e.g. ".groovy", ".json"
     */
    protected AbstractScriptWatcher(String... fileExtensions) {
        this.fileExtensions = fileExtensions;
    }

    /**
     * Begin monitoring {@code directory}. Creates the directory if absent,
     * performs an initial scan, and starts the background watcher thread.
     */
    protected void startWatching(Path directory) {
        this.watchDirectory = directory;
        this.watchExecutor = Executors.newSingleThreadExecutor();

        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            this.watchService = FileSystems.getDefault().newWatchService();
            directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            scanDirectory();
            startWatcherThread();

            log.info("{} activated. Monitoring: {}", getClass().getSimpleName(), directory);
        } catch (IOException e) {
            log.error("Failed to initialize script watcher for {}", directory, e);
        }
    }

    /**
     * Stop monitoring and release resources.
     */
    protected void stopWatching() {
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.warn("Error closing watch service", e);
            }
        }
        if (watchExecutor != null) {
            watchExecutor.shutdownNow();
        }
        onShutdown();
        log.info("{} deactivated", getClass().getSimpleName());
    }

    /**
     * Scan the watched directory and invoke {@link #onFileChanged(Path)} for
     * every file whose extension matches.
     */
    protected void scanDirectory() {
        try (var stream = Files.newDirectoryStream(watchDirectory)) {
            for (Path path : stream) {
                if (acceptsFile(path)) {
                    onFileChanged(path);
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan directory {}", watchDirectory, e);
        }
    }

    protected Path getWatchDirectory() {
        return watchDirectory;
    }

    protected boolean acceptsFile(Path path) {
        String name = path.getFileName().toString();
        for (String ext : fileExtensions) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }

    /**
     * Called when a matching file is created or modified.
     */
    protected abstract void onFileChanged(Path path);

    /**
     * Called when a matching file is deleted.
     */
    protected abstract void onFileRemoved(Path path);

    /**
     * Called during {@link #stopWatching()} to allow subclasses to clean up
     * registrations, caches, etc.
     */
    protected void onShutdown() {
    }

    private void startWatcherThread() {
        watchExecutor.submit(() -> {
            try {
                WatchKey key;
                while ((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path fileName = (Path) event.context();
                        Path fullPath = watchDirectory.resolve(fileName);

                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            if (acceptsFile(fullPath)) {
                                log.info("Detected file deletion: {}", fileName);
                                onFileRemoved(fullPath);
                            }
                        } else if (acceptsFile(fullPath)) {
                            log.info("Detected file change: {}", fileName);
                            Thread.sleep(200);
                            onFileChanged(fullPath);
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Error in script watcher", e);
            }
        });
    }
}
