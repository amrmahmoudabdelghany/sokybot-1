package org.sokybot.machinepages.internal;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.engine.api.scripting.IScriptEngine;
import org.sokybot.machinepages.api.IScriptedPage;
import org.sokybot.runtime.IMachineContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component(service = ScriptPageLoader.class, immediate = true)
public class ScriptPageLoader {

    private static final Logger log = LoggerFactory.getLogger(ScriptPageLoader.class);
    private static final String DEFAULT_PAGES_DIR = "scripts/pages";

    private final Map<String, PageDefinition> pages = new ConcurrentHashMap<>();
    private final ExecutorService watchExecutor = Executors.newSingleThreadExecutor();
    private final List<Consumer<String>> pageListeners = new CopyOnWriteArrayList<>();

    private IScriptEngine scriptEngine;
    private Path pagesDirectory;
    private WatchService watchService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Reference
    protected void setScriptEngine(IScriptEngine engine) {
        this.scriptEngine = engine;
    }

    @Activate
    protected void activate() {
        this.pagesDirectory = Paths.get(System.getProperty("sokybot.pages.dir", DEFAULT_PAGES_DIR));

        try {
            if (!Files.exists(pagesDirectory)) {
                Files.createDirectories(pagesDirectory);
            }

            this.watchService = FileSystems.getDefault().newWatchService();
            this.pagesDirectory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            scanAndLoad();
            startWatcher();

            log.info("Script Page Loader activated. Monitoring: {}", pagesDirectory);
        } catch (IOException e) {
            log.error("Failed to initialize script page watcher", e);
        }
    }

    @Deactivate
    protected void deactivate() {
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.warn("Error closing watch service", e);
            }
        }
        watchExecutor.shutdownNow();
        pages.clear();
        log.info("Script Page Loader deactivated");
    }

    public void addPageListener(Consumer<String> listener) {
        pageListeners.add(listener);
    }

    public void removePageListener(Consumer<String> listener) {
        pageListeners.remove(listener);
    }

    public Set<String> getAvailablePages() {
        return Collections.unmodifiableSet(pages.keySet());
    }

    public void scanAndLoad() {
        try (var stream = Files.newDirectoryStream(pagesDirectory)) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (fileName.endsWith(".groovy") || fileName.endsWith(".json")) {
                    updatePageDefinition(path);
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan pages directory", e);
        }
    }

    private void updatePageDefinition(Path path) {
        String fileName = path.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

        PageDefinition def = pages.computeIfAbsent(baseName, k -> new PageDefinition());

        if (fileName.endsWith(".groovy")) {
            def.scriptPath = path;
            log.debug("Detected script for page: {}", baseName);
        } else if (fileName.endsWith(".json")) {
            def.schemaPath = path;
            log.debug("Detected schema for page: {}", baseName);
        }

        notifyListeners(baseName);
    }

    private void removePageDefinition(Path path) {
        String fileName = path.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

        PageDefinition def = pages.get(baseName);
        if (def != null) {
            if (fileName.endsWith(".groovy")) {
                def.scriptPath = null;
            } else if (fileName.endsWith(".json")) {
                def.schemaPath = null;
            }

            if (def.isEmpty()) {
                pages.remove(baseName);
            }
            notifyListeners(baseName);
        }
    }

    private void notifyListeners(String pageName) {
        pageListeners.forEach(l -> {
            try {
                l.accept(pageName);
            } catch (Exception e) {
                log.error("Error in page listener for {}: {}", pageName, e.getMessage());
            }
        });
    }

    public Optional<IScriptedPage> createPage(String pageName, IMachineContext context) {
        PageDefinition def = pages.get(pageName);
        if (def == null || def.scriptPath == null) {
            return Optional.empty();
        }

        try {
            String scriptContent = Files.readString(def.scriptPath);
            Object result = scriptEngine.execute(scriptContent, Collections.emptyMap());

            if (result instanceof IScriptedPage) {
                IScriptedPage page = (IScriptedPage) result;

                // If we have a separate JSON schema, inject it
                if (def.schemaPath != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> schema = mapper.readValue(Files.readAllBytes(def.schemaPath), Map.class);
                    log.debug("Loaded schema from script loader: {}", schema.size());
                }

                page.init(context);
                return Optional.of(page);
            } else if (result instanceof Class && IScriptedPage.class.isAssignableFrom((Class<?>) result)) {
                IScriptedPage page = (IScriptedPage) ((Class<?>) result).getDeclaredConstructor().newInstance();
                page.init(context);
                return Optional.of(page);
            } else {
                log.error("Script {} did not return an IScriptedPage or its class", def.scriptPath.getFileName());
            }
        } catch (Exception e) {
            log.error("Failed to create page {}: {}", pageName, e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Map<String, Object> loadSchema(String pageName) {
        PageDefinition def = pages.get(pageName);
        if (def != null && def.schemaPath != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> schema = (Map<String, Object>) mapper.readValue(Files.readAllBytes(def.schemaPath),
                        Map.class);
                return schema;
            } catch (IOException e) {
                log.error("Failed to load schema for {}: {}", pageName, e.getMessage());
            }
        }
        return Collections.emptyMap();
    }

    private void startWatcher() {
        watchExecutor.submit(() -> {
            try {
                WatchKey key;
                while ((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path fileName = (Path) event.context();
                        Path fullPath = pagesDirectory.resolve(fileName);

                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            removePageDefinition(fullPath);
                        } else if (fileName.toString().endsWith(".groovy") || fileName.toString().endsWith(".json")) {
                            // Brief delay to ensure file write is complete
                            Thread.sleep(200);
                            updatePageDefinition(fullPath);
                        }
                    }
                    key.reset();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Error in script page watcher", e);
            }
        });
    }

    private static class PageDefinition {
        Path scriptPath;
        Path schemaPath;

        boolean isEmpty() {
            return scriptPath == null && schemaPath == null;
        }
    }
}
