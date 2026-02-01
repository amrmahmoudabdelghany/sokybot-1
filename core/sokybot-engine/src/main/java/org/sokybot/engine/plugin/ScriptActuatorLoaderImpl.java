package org.sokybot.engine.plugin;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.scripting.IScriptEngine;

/**
 * Implementation of IScriptActuatorLoader.
 * Monitors a directory for .groovy scripts and registers them as IActuator
 * services.
 */
@Component(service = IScriptActuatorLoader.class, immediate = true)
public class ScriptActuatorLoaderImpl implements IScriptActuatorLoader {

    private static final Logger log = LoggerFactory.getLogger(ScriptActuatorLoaderImpl.class);
    private static final String DEFAULT_SCRIPTS_DIR = "scripts/actuators";

    private final Map<Path, ServiceRegistration<IActuator>> registrations = new ConcurrentHashMap<>();
    private final ExecutorService watchExecutor = Executors.newSingleThreadExecutor();

    private IScriptEngine scriptEngine;
    private BundleContext bundleContext;
    private Path scriptsDirectory;
    private WatchService watchService;

    @Reference
    protected void setScriptEngine(IScriptEngine engine) {
        this.scriptEngine = engine;
    }

    @Activate
    protected void activate(BundleContext context) {
        this.bundleContext = context;
        this.scriptsDirectory = Paths.get(System.getProperty("sokybot.scripts.dir", DEFAULT_SCRIPTS_DIR));

        try {
            if (!Files.exists(scriptsDirectory)) {
                Files.createDirectories(scriptsDirectory);
            }

            this.watchService = FileSystems.getDefault().newWatchService();
            this.scriptsDirectory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            scanAndLoad();
            startWatcher();

            log.info("Script Actuator Loader activated. Monitoring: {}", scriptsDirectory);
        } catch (IOException e) {
            log.error("Failed to initialize script watcher", e);
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
        registrations.values().forEach(ServiceRegistration::unregister);
        registrations.clear();
        log.info("Script Actuator Loader deactivated");
    }

    @Override
    public int scanAndLoad() {
        try (var stream = Files.newDirectoryStream(scriptsDirectory, "*.groovy")) {
            int count = 0;
            for (Path path : stream) {
                if (loadScript(path).isPresent()) {
                    count++;
                }
            }
            return count;
        } catch (IOException e) {
            log.error("Failed to scan scripts directory", e);
            return 0;
        }
    }

    @Override
    public Optional<IActuator> loadScript(Path scriptFile) {
        log.info("Loading script actuator: {}", scriptFile.getFileName());

        try {
            // Unload existing if present
            unloadScriptByPath(scriptFile);

            String content = Files.readString(scriptFile);
            Object result = scriptEngine.execute(content, Collections.emptyMap());

            if (result instanceof IActuator) {
                IActuator actuator = (IActuator) result;

                // Register as OSGi service
                Dictionary<String, Object> props = new Hashtable<>();
                props.put("script.path", scriptFile.toString());
                props.put("actuator.name", actuator.getName());

                ServiceRegistration<IActuator> reg = bundleContext.registerService(IActuator.class, actuator, props);
                registrations.put(scriptFile, reg);

                log.info("Successfully registered scripted actuator: {}", actuator.getName());
                return Optional.of(actuator);
            } else {
                log.error("Script {} did not return an instance of IActuator", scriptFile.getFileName());
            }
        } catch (Exception e) {
            log.error("Failed to load script {}: {}", scriptFile.getFileName(), e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public boolean unloadScript(String name) {
        Path targetPath = registrations.entrySet().stream()
                .filter(e -> {
                    try {
                        return name
                                .equals(((IActuator) bundleContext.getService(e.getValue().getReference())).getName());
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (targetPath != null) {
            return unloadScriptByPath(targetPath);
        }
        return false;
    }

    private boolean unloadScriptByPath(Path path) {
        ServiceRegistration<IActuator> reg = registrations.remove(path);
        if (reg != null) {
            try {
                reg.unregister();
                return true;
            } catch (IllegalStateException e) {
                // Already unregistered
            }
        }
        return false;
    }

    @Override
    public Path getScriptsDirectory() {
        return scriptsDirectory;
    }

    @Override
    public void reloadAll() {
        registrations.keySet().forEach(this::loadScript);
    }

    private void startWatcher() {
        watchExecutor.submit(() -> {
            try {
                WatchKey key;
                while ((key = watchService.take()) != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path fileName = (Path) event.context();
                        if (fileName.toString().endsWith(".groovy")) {
                            Path fullPath = scriptsDirectory.resolve(fileName);

                            if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                                log.info("Detected script deletion: {}", fileName);
                                unloadScriptByPath(fullPath);
                            } else {
                                log.info("Detected script change: {}", fileName);
                                // Brief delay to ensure file write is complete
                                Thread.sleep(200);
                                loadScript(fullPath);
                            }
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
