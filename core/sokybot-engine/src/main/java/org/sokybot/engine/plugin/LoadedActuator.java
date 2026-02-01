package org.sokybot.engine.plugin;

import java.nio.file.Path;
import java.time.Instant;

import org.sokybot.engine.api.extension.ActuatorDescriptor;
import org.sokybot.engine.api.extension.IActuator;

/**
 * Represents a loaded actuator with metadata about how it was loaded.
 */
public class LoadedActuator {

    private final IActuator actuator;
    private final Path sourcePath;
    private final Instant loadedAt;
    private final LoadSource loadSource;
    private boolean enabled;

    /**
     * Source type for the actuator.
     */
    public enum LoadSource {
        /** Loaded from an OSGi bundle */
        OSGI_BUNDLE,
        /** Loaded from a JAR file */
        JAR_FILE,
        /** Loaded from a directory */
        DIRECTORY,
        /** Built-in actuator */
        BUILTIN
    }

    public LoadedActuator(IActuator actuator, Path sourcePath, LoadSource loadSource) {
        this.actuator = actuator;
        this.sourcePath = sourcePath;
        this.loadSource = loadSource;
        this.loadedAt = Instant.now();
        this.enabled = true;
    }

    /**
     * Create a loaded actuator from OSGi discovery.
     */
    public static LoadedActuator fromOsgi(IActuator actuator) {
        return new LoadedActuator(actuator, null, LoadSource.OSGI_BUNDLE);
    }

    /**
     * Create a loaded actuator from a JAR file.
     */
    public static LoadedActuator fromJar(IActuator actuator, Path jarPath) {
        return new LoadedActuator(actuator, jarPath, LoadSource.JAR_FILE);
    }

    /**
     * Create a built-in actuator.
     */
    public static LoadedActuator builtin(IActuator actuator) {
        return new LoadedActuator(actuator, null, LoadSource.BUILTIN);
    }

    public IActuator getActuator() {
        return actuator;
    }

    public String getName() {
        return actuator.getName();
    }

    public ActuatorDescriptor getDescriptor() {
        return actuator.getDescriptor();
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public Instant getLoadedAt() {
        return loadedAt;
    }

    public LoadSource getLoadSource() {
        return loadSource;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return String.format("LoadedActuator{name='%s', source=%s, enabled=%s}",
                getName(), loadSource, enabled);
    }
}
