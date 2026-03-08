package org.sokybot.machinepages;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.script.AbstractScriptWatcher;
import org.sokybot.engine.api.scripting.IScriptEngine;
import org.sokybot.machinepages.api.IScriptedPage;
import org.sokybot.runtime.IMachineContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Monitors a directory for Groovy page scripts and their companion JSON
 * schemas. File watching is handled by {@link AbstractScriptWatcher}.
 */
@Component(service = ScriptPageLoader.class, immediate = true)
public class ScriptPageLoader extends AbstractScriptWatcher {

    private static final String DEFAULT_PAGES_DIR = "scripts/pages";

    private final Map<String, PageDefinition> pages = new ConcurrentHashMap<>();
    private final List<Consumer<String>> pageListeners = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    private IScriptEngine scriptEngine;

    public ScriptPageLoader() {
        super(".groovy", ".json");
    }

    @Reference
    protected void setScriptEngine(IScriptEngine engine) {
        this.scriptEngine = engine;
    }

    @Activate
    protected void activate() {
        String dirProp = System.getProperty("sokybot.pages.dir");
        Path dir = dirProp != null && !dirProp.isEmpty()
                ? Paths.get(dirProp)
                : Paths.get(System.getProperty("user.dir", ".")).resolve(DEFAULT_PAGES_DIR);
        try {
            dir = dir.toAbsolutePath().normalize();
        } catch (Exception ignored) {
        }
        startWatching(dir);
        log.info("ScriptPageLoader: pages directory={}, available pages after scan={} {}", dir, pages.size(), pages.keySet());
    }

    @Deactivate
    protected void deactivate() {
        stopWatching();
    }

    @Override
    protected void onShutdown() {
        pages.clear();
    }

    @Override
    protected void onFileChanged(Path path) {
        updatePageDefinition(path);
    }

    @Override
    protected void onFileRemoved(Path path) {
        removePageDefinition(path);
    }

    // ---- Page listener support ----

    public void addPageListener(Consumer<String> listener) {
        pageListeners.add(listener);
    }

    public void removePageListener(Consumer<String> listener) {
        pageListeners.remove(listener);
    }

    public Set<String> getAvailablePages() {
        return Collections.unmodifiableSet(pages.keySet());
    }

    /**
     * Force a full rescan of the pages directory and notify listeners.
     * Used by dev-shell script-reload command.
     */
    public void scanAndLoad() {
        if (getWatchDirectory() != null) {
            scanDirectory();
            getAvailablePages().forEach(this::notifyListeners);
        }
    }

    // ---- Page definition management ----

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

    // ---- Page creation ----

    public Optional<IScriptedPage> createPage(String pageName, IMachineContext context) {
        PageDefinition def = pages.get(pageName);
        if (def == null || def.scriptPath == null) {
            return Optional.empty();
        }

        try {
            String scriptContent = Files.readString(def.scriptPath);

            var errors = scriptEngine.validate(scriptContent);
            if (!errors.isEmpty()) {
                log.error("Compilation errors in page '{}': {}", pageName, errors);
                return Optional.empty();
            }

            Object result = scriptEngine.execute(scriptContent, Collections.emptyMap());

            if (result instanceof IScriptedPage) {
                IScriptedPage page = (IScriptedPage) result;

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
                log.error("Script {} did not return an IScriptedPage instance or class", def.scriptPath.getFileName());
            }
        } catch (Exception e) {
            log.error("Failed to create page {}: {}", pageName, e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ---- Schema loading ----

    public Map<String, Object> loadSchema(String pageName) {
        PageDefinition def = pages.get(pageName);
        if (def != null && def.schemaPath != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> schema = (Map<String, Object>) mapper.readValue(
                        Files.readAllBytes(def.schemaPath), Map.class);
                resolveRefs(schema, new HashSet<>());
                return schema;
            } catch (IOException e) {
                log.error("Failed to load schema for {}: {}", pageName, e.getMessage());
            }
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private void resolveRefs(Map<String, Object> node, Set<String> visited) {
        Object childrenObj = node.get("children");
        if (!(childrenObj instanceof List)) return;

        List<Object> children = (List<Object>) childrenObj;
        for (int i = 0; i < children.size(); i++) {
            Object child = children.get(i);
            if (!(child instanceof Map)) continue;

            Map<String, Object> childMap = (Map<String, Object>) child;
            String ref = (String) childMap.get("$ref");
            if (ref != null) {
                if (visited.contains(ref)) {
                    log.warn("Circular $ref detected: {}", ref);
                    continue;
                }
                Path refPath = getWatchDirectory().resolve(ref);
                try {
                    Map<String, Object> resolved = (Map<String, Object>) mapper.readValue(
                            Files.readAllBytes(refPath), Map.class);
                    childMap.remove("$ref");
                    for (Map.Entry<String, Object> entry : childMap.entrySet()) {
                        resolved.putIfAbsent(entry.getKey(), entry.getValue());
                    }
                    children.set(i, resolved);
                    visited.add(ref);
                    resolveRefs(resolved, visited);
                    visited.remove(ref);
                } catch (IOException e) {
                    log.error("Failed to resolve $ref '{}': {}", ref, e.getMessage());
                }
            } else {
                resolveRefs(childMap, visited);
            }
        }
    }

    private static class PageDefinition {
        Path scriptPath;
        Path schemaPath;

        boolean isEmpty() {
            return scriptPath == null && schemaPath == null;
        }
    }
}
