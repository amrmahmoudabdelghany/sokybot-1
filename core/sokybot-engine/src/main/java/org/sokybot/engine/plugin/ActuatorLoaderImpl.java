package org.sokybot.engine.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.engine.api.extension.ActuatorDescriptor;
import org.sokybot.engine.api.extension.IActuator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Implementation of actuator loader.
 * Discovers and loads actuator plugins from JAR files.
 */
@Component(service = IActuatorLoader.class)
public class ActuatorLoaderImpl implements IActuatorLoader {

    private static final Logger log = LoggerFactory.getLogger(ActuatorLoaderImpl.class);

    private static final String MANIFEST_FILE = "actuator.yaml";
    private static final String DEFAULT_PLUGIN_DIR = "plugins/actuators";

    private final Map<String, LoadedActuator> loadedActuators = new ConcurrentHashMap<>();
    private final Map<String, URLClassLoader> classLoaders = new ConcurrentHashMap<>();

    private ObjectMapper yamlMapper;
    private Path pluginDirectory;

    @Activate
    protected void activate() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.pluginDirectory = Paths.get(System.getProperty("sokybot.plugins.dir", DEFAULT_PLUGIN_DIR));

        log.info("Actuator Loader activated, plugin directory: {}", pluginDirectory);

        // Auto-load on startup if directory exists
        if (Files.isDirectory(pluginDirectory)) {
            loadFromDirectory(pluginDirectory);
        }
    }

    @Deactivate
    protected void deactivate() {
        // Close all class loaders
        for (Map.Entry<String, URLClassLoader> entry : classLoaders.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                log.warn("Failed to close classloader for {}: {}", entry.getKey(), e.getMessage());
            }
        }
        classLoaders.clear();
        loadedActuators.clear();
        log.info("Actuator Loader deactivated");
    }

    /**
     * Register actuators discovered via OSGi.
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindActuator(IActuator actuator) {
        String name = actuator.getName();
        if (!loadedActuators.containsKey(name)) {
            loadedActuators.put(name, LoadedActuator.fromOsgi(actuator));
            log.info("Registered OSGi actuator: {}", name);
        }
    }

    protected void unbindActuator(IActuator actuator) {
        String name = actuator.getName();
        LoadedActuator loaded = loadedActuators.get(name);
        if (loaded != null && loaded.getLoadSource() == LoadedActuator.LoadSource.OSGI_BUNDLE) {
            loadedActuators.remove(name);
            log.info("Unregistered OSGi actuator: {}", name);
        }
    }

    @Override
    public int loadFromDirectory(Path directory) {
        if (!Files.isDirectory(directory)) {
            log.warn("Plugin directory does not exist: {}", directory);
            return 0;
        }

        int count = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.jar")) {
            for (Path jarPath : stream) {
                Optional<IActuator> loaded = loadFromJar(jarPath);
                if (loaded.isPresent()) {
                    count++;
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan plugin directory: {}", e.getMessage(), e);
        }

        log.info("Loaded {} actuators from {}", count, directory);
        return count;
    }

    @Override
    public Optional<IActuator> loadFromJar(Path jarFile) {
        if (!Files.isRegularFile(jarFile) || !jarFile.toString().endsWith(".jar")) {
            log.warn("Invalid JAR file: {}", jarFile);
            return Optional.empty();
        }

        try (JarFile jar = new JarFile(jarFile.toFile())) {
            // Find manifest
            JarEntry manifestEntry = jar.getJarEntry(MANIFEST_FILE);
            if (manifestEntry == null) {
                log.warn("No {} found in {}", MANIFEST_FILE, jarFile);
                return Optional.empty();
            }

            // Parse manifest
            ActuatorManifest manifest;
            try (InputStream is = jar.getInputStream(manifestEntry)) {
                manifest = yamlMapper.readValue(is, ActuatorManifest.class);
            }

            if (!manifest.isValid()) {
                log.error("Invalid manifest in {}: missing name or mainClass", jarFile);
                return Optional.empty();
            }

            // Check for duplicate
            if (loadedActuators.containsKey(manifest.getName())) {
                log.warn("Actuator '{}' already loaded, skipping {}", manifest.getName(), jarFile);
                return Optional.empty();
            }

            // Create classloader
            URL jarUrl = jarFile.toUri().toURL();
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[] { jarUrl },
                    getClass().getClassLoader());

            // Load main class
            Class<?> mainClass = classLoader.loadClass(manifest.getMainClass());
            if (!IActuator.class.isAssignableFrom(mainClass)) {
                log.error("Main class {} does not implement IActuator", manifest.getMainClass());
                classLoader.close();
                return Optional.empty();
            }

            // Instantiate
            @SuppressWarnings("unchecked")
            Class<? extends IActuator> actuatorClass = (Class<? extends IActuator>) mainClass;
            IActuator actuator = actuatorClass.getDeclaredConstructor().newInstance();

            // Wrap with metadata
            IActuator wrappedActuator = wrapWithDescriptor(actuator, manifest);

            // Register
            LoadedActuator loaded = LoadedActuator.fromJar(wrappedActuator, jarFile);
            loadedActuators.put(manifest.getName(), loaded);
            classLoaders.put(manifest.getName(), classLoader);

            log.info("Loaded actuator '{}' v{} from {}",
                    manifest.getName(), manifest.getVersion(), jarFile.getFileName());

            return Optional.of(wrappedActuator);

        } catch (Exception e) {
            log.error("Failed to load actuator from {}: {}", jarFile, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Wrap an actuator to use manifest-based descriptor.
     */
    private IActuator wrapWithDescriptor(IActuator actuator, ActuatorManifest manifest) {
        ActuatorDescriptor descriptor = ActuatorDescriptor.builder(manifest.getName())
                .displayName(manifest.getDisplayName())
                .description(manifest.getDescription())
                .version(manifest.getVersion())
                .author(manifest.getAuthor())
                .dependencies(manifest.getDependencies())
                .tags(manifest.getTags())
                .build();

        return new IActuator() {
            @Override
            public String getName() {
                return actuator.getName();
            }

            @Override
            public void initialize(org.sokybot.engine.api.extension.IActuatorContext context) {
                actuator.initialize(context);
            }

            @Override
            public void shutdown(org.sokybot.engine.api.extension.IActuatorContext context) {
                actuator.shutdown(context);
            }

            @Override
            public ActuatorDescriptor getDescriptor() {
                return descriptor;
            }
        };
    }

    @Override
    public List<LoadedActuator> getLoadedActuators() {
        return new ArrayList<>(loadedActuators.values());
    }

    @Override
    public boolean unload(String name) {
        LoadedActuator loaded = loadedActuators.remove(name);
        if (loaded == null) {
            return false;
        }

        // Close classloader if it was a JAR-loaded actuator
        URLClassLoader classLoader = classLoaders.remove(name);
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException e) {
                log.warn("Failed to close classloader for {}: {}", name, e.getMessage());
            }
        }

        log.info("Unloaded actuator: {}", name);
        return true;
    }

    @Override
    public Optional<LoadedActuator> getActuator(String name) {
        return Optional.ofNullable(loadedActuators.get(name));
    }

    @Override
    public int reloadAll() {
        // Unload JAR-based actuators
        List<String> jarActuators = new ArrayList<>();
        for (Map.Entry<String, LoadedActuator> entry : loadedActuators.entrySet()) {
            if (entry.getValue().getLoadSource() == LoadedActuator.LoadSource.JAR_FILE) {
                jarActuators.add(entry.getKey());
            }
        }

        for (String name : jarActuators) {
            unload(name);
        }

        // Reload from directory
        return loadFromDirectory(pluginDirectory);
    }

    @Override
    public Path getPluginDirectory() {
        return pluginDirectory;
    }
}
