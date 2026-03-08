package org.sokybot.engine.plugin;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.commons.script.AbstractScriptWatcher;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.scripting.IScriptEngine;

/**
 * Monitors a directory for .groovy scripts and registers them as IActuator
 * OSGi services. File watching, directory creation, and debounce logic are
 * handled by {@link AbstractScriptWatcher}.
 */
@Component(service = IScriptActuatorLoader.class, immediate = true)
public class ScriptActuatorLoaderImpl extends AbstractScriptWatcher implements IScriptActuatorLoader {

    private static final String DEFAULT_SCRIPTS_DIR = "scripts/actuators";

    private final Map<Path, ServiceRegistration<IActuator>> registrations = new ConcurrentHashMap<>();

    private IScriptEngine scriptEngine;
    private BundleContext bundleContext;

    public ScriptActuatorLoaderImpl() {
        super(".groovy");
    }

    @Reference
    protected void setScriptEngine(IScriptEngine engine) {
        this.scriptEngine = engine;
    }

    @Activate
    protected void activate(BundleContext context) {
        this.bundleContext = context;
        Path dir = Paths.get(System.getProperty("sokybot.scripts.dir", DEFAULT_SCRIPTS_DIR));
        startWatching(dir);
    }

    @Deactivate
    protected void deactivate() {
        stopWatching();
    }

    @Override
    protected void onShutdown() {
        registrations.values().forEach(reg -> {
            try { reg.unregister(); } catch (IllegalStateException ignored) {}
        });
        registrations.clear();
    }

    @Override
    protected void onFileChanged(Path path) {
        loadScript(path);
    }

    @Override
    protected void onFileRemoved(Path path) {
        unloadScriptByPath(path);
    }

    @Override
    public int scanAndLoad() {
        try (var stream = Files.newDirectoryStream(getWatchDirectory(), "*.groovy")) {
            int count = 0;
            for (Path path : stream) {
                if (loadScript(path).isPresent()) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            log.error("Failed to scan scripts directory", e);
            return 0;
        }
    }

    @Override
    public Optional<IActuator> loadScript(Path scriptFile) {
        log.info("Loading script actuator: {}", scriptFile.getFileName());

        try {
            String content = Files.readString(scriptFile);

            var errors = scriptEngine.validate(content);
            if (!errors.isEmpty()) {
                log.error("Compilation errors in {}: {}", scriptFile.getFileName(), errors);
                return Optional.empty();
            }

            unloadScriptByPath(scriptFile);

            Object result = scriptEngine.execute(content, Collections.emptyMap());

            if (result instanceof IActuator) {
                IActuator actuator = (IActuator) result;

                Dictionary<String, Object> props = new Hashtable<>();
                props.put("script.path", scriptFile.toString());
                props.put("actuator.name", actuator.getName());

                ServiceRegistration<IActuator> reg = bundleContext.registerService(IActuator.class, actuator, props);
                registrations.put(scriptFile, reg);

                log.info("Successfully registered scripted actuator: {}", actuator.getName());
                return Optional.of(actuator);
            } else if (result instanceof Class && IActuator.class.isAssignableFrom((Class<?>) result)) {
                IActuator actuator = (IActuator) ((Class<?>) result).getDeclaredConstructor().newInstance();

                Dictionary<String, Object> props = new Hashtable<>();
                props.put("script.path", scriptFile.toString());
                props.put("actuator.name", actuator.getName());

                ServiceRegistration<IActuator> reg = bundleContext.registerService(IActuator.class, actuator, props);
                registrations.put(scriptFile, reg);

                log.info("Successfully registered scripted actuator (from class): {}", actuator.getName());
                return Optional.of(actuator);
            } else {
                log.error("Script {} did not return an IActuator instance or class", scriptFile.getFileName());
            }
        } catch (Exception e) {
            log.error("Failed to load script {}: {}", scriptFile.getFileName(), e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public boolean unloadScript(String name) {
        Path targetPath = registrations.entrySet().stream()
                .filter(e -> {
                    try {
                        return name.equals(
                                ((IActuator) bundleContext.getService(e.getValue().getReference())).getName());
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
        return getWatchDirectory();
    }

    @Override
    public void reloadAll() {
        registrations.keySet().forEach(this::loadScript);
    }
}
