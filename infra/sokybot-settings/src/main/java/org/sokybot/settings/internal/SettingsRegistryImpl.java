package org.sokybot.settings.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Central registry implementation for settings scopes.
 */
@Slf4j
@Component(service = ISettingsRegistry.class)
public class SettingsRegistryImpl implements ISettingsRegistry {

    private static final String DATA_DIR = "sokybot-data";
    private static final String SETTINGS_DIR = "settings";

    private org.sokybot.settings.security.ICredentialEncryptor credentialEncryptor;

    private final Map<String, ScopeRegistration<?>> registrations = new ConcurrentHashMap<>();
    private final Map<String, ISettingsProvider<?>> providers = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Reference
    public void setCredentialEncryptor(org.sokybot.settings.security.ICredentialEncryptor credentialEncryptor) {
        this.credentialEncryptor = credentialEncryptor;
    }

    public SettingsRegistryImpl() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR, SETTINGS_DIR));
        } catch (Exception e) {
            log.error("Failed to create settings directory", e);
        }
    }

    @Override
    public <T> void register(String scope, Class<T> type, Supplier<T> defaultsFactory) {
        if (scope == null || type == null || defaultsFactory == null) {
            throw new IllegalArgumentException("Scope, type, and defaults factory must not be null");
        }

        registrations.put(scope, new ScopeRegistration<>(type, defaultsFactory));
        log.info("Registered settings scope '{}' with type {}", scope, type.getSimpleName());
        resolveDeferredProviders(scope);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ISettingsProvider<T> getProvider(String groupName, String machineName, String scope, Class<T> type) {
        String key = groupName + "/" + machineName + "/" + scope;
        ScopeRegistration<?> registration = registrations.get(scope);

        if (registration == null) {
            ISettingsProvider<?> provider = providers.computeIfAbsent(
                    key,
                    k -> new DeferredSettingsProvider<>(scope, key));
            ScopeRegistration<?> registrationAfter = registrations.get(scope);
            if (registrationAfter != null && provider instanceof DeferredSettingsProvider<?>) {
                bindDeferredProvider((DeferredSettingsProvider<Object>) provider, groupName, machineName, scope, registrationAfter);
            }
            return (ISettingsProvider<T>) provider;
        }

        if (!isTypeCompatible(type, registration.type)) {
            throw new IllegalArgumentException("Type mismatch for scope " + scope
                    + ": expected " + registration.type.getName()
                    + " (loader=" + String.valueOf(registration.type.getClassLoader()) + ")"
                    + ", got " + type.getName()
                    + " (loader=" + String.valueOf(type.getClassLoader()) + ")");
        }

        ISettingsProvider<?> existingProvider = providers.get(key);
        if (existingProvider instanceof DeferredSettingsProvider<?>) {
            bindDeferredProvider((DeferredSettingsProvider<Object>) existingProvider, groupName, machineName, scope, registration);
            return (ISettingsProvider<T>) existingProvider;
        }
        if (existingProvider != null) {
            return (ISettingsProvider<T>) existingProvider;
        }

        return (ISettingsProvider<T>) providers.computeIfAbsent(key, k -> {
            Path settingsFile = getSettingsFile(groupName, machineName, scope);
            return new SettingsProviderImpl<>(
                    (Class<T>) registration.type,
                    (Supplier<T>) registration.defaultsFactory,
                    settingsFile,
                    objectMapper,
                    credentialEncryptor);
        });
    }

    @SuppressWarnings("unchecked")
    private void resolveDeferredProviders(String scope) {
        ScopeRegistration<?> registration = registrations.get(scope);
        if (registration == null) {
            return;
        }

        String suffix = "/" + scope;
        providers.forEach((key, provider) -> {
            if (!key.endsWith(suffix) || !(provider instanceof DeferredSettingsProvider<?>)) {
                return;
            }

            String[] parts = key.split("/", 3);
            if (parts.length != 3) {
                log.warn("Unexpected provider key format: {}", key);
                return;
            }

            bindDeferredProvider(
                    (DeferredSettingsProvider<Object>) provider,
                    parts[0],
                    parts[1],
                    scope,
                    registration);
        });
    }

    @SuppressWarnings("unchecked")
    private void bindDeferredProvider(
            DeferredSettingsProvider<Object> deferred,
            String groupName,
            String machineName,
            String scope,
            ScopeRegistration<?> registration) {
        Path settingsFile = getSettingsFile(groupName, machineName, scope);
        SettingsProviderImpl<Object> concreteProvider = new SettingsProviderImpl<>(
                (Class<Object>) registration.type,
                (Supplier<Object>) registration.defaultsFactory,
                settingsFile,
                objectMapper,
                credentialEncryptor);
        deferred.bind(concreteProvider);
    }

    @Override
    public Set<String> getRegisteredScopes() {
        return registrations.keySet();
    }

    private Path getSettingsFile(String groupName, String machineName, String scope) {
        String safeGroup = sanitize(groupName);
        String safeMachine = sanitize(machineName);
        String safeScope = sanitize(scope);

        Path machineDir = Paths.get(DATA_DIR, SETTINGS_DIR, safeGroup, safeMachine);
        try {
            Files.createDirectories(machineDir);
        } catch (Exception e) {
            log.error("Failed to create machine settings directory", e);
        }

        return machineDir.resolve(safeScope + ".json");
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private boolean isTypeCompatible(Class<?> requestedType, Class<?> registeredType) {
        if (requestedType == null || registeredType == null) {
            return false;
        }
        if (requestedType == Object.class) {
            return true;
        }
        if (requestedType.equals(registeredType)) {
            return true;
        }
        if (requestedType.getName().equals(registeredType.getName())) {
            // Hot-reloaded Groovy scripts can produce same FQCN under different classloaders.
            // Treat these as compatible for scope lookup.
            log.debug("Type compatibility by FQCN match: {} (requestedLoader={}) vs {} (registeredLoader={})",
                    requestedType.getName(),
                    String.valueOf(requestedType.getClassLoader()),
                    registeredType.getName(),
                    String.valueOf(registeredType.getClassLoader()));
            return true;
        }
        return requestedType.isAssignableFrom(registeredType)
                || registeredType.isAssignableFrom(requestedType);
    }

    private static class ScopeRegistration<T> {
        final Class<T> type;
        final Supplier<T> defaultsFactory;

        ScopeRegistration(Class<T> type, Supplier<T> defaultsFactory) {
            this.type = type;
            this.defaultsFactory = defaultsFactory;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> readRawSettings(String groupName, String machineName, String scope) {
        if (groupName == null || machineName == null || scope == null) {
            return Collections.emptyMap();
        }
        Path settingsFile = getSettingsFile(groupName, machineName, scope);
        if (!Files.exists(settingsFile)) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(settingsFile.toFile(), Map.class);
            return map != null ? map : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Failed to read raw settings from {}", settingsFile, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public void writeRawSettings(String groupName, String machineName, String scope, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        Path settingsFile = getSettingsFile(groupName, machineName, scope);

        try {
            // Use existing file content as base if it exists, to avoid overwriting
            // unrelated fields
            Map<String, Object> mergedData = new ConcurrentHashMap<>();
            if (Files.exists(settingsFile)) {
                try {
                    Map<String, Object> existing = objectMapper.readValue(settingsFile.toFile(), Map.class);
                    if (existing != null) {
                        mergedData.putAll(existing);
                    }
                } catch (Exception e) {
                    log.warn("Failed to read existing settings from {}, overwriting", settingsFile, e);
                }
            }

            // Encrypt passwords/passcodes dynamically if encryptor is available
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof String && credentialEncryptor != null && credentialEncryptor.isUnlocked()) {
                    String lowerKey = key.toLowerCase();
                    // Basic heuristic for typical Sokybot secret fields
                    if (lowerKey.contains("password") || lowerKey.contains("passcode") || lowerKey.contains("secret")) {
                        try {
                            value = credentialEncryptor.encrypt((String) value);
                        } catch (Exception e) {
                            log.error("Failed to encrypt dynamic field: {}", key, e);
                        }
                    }
                }
                mergedData.put(key, value);
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(settingsFile.toFile(), mergedData);
            log.info("Dynamically wrote raw settings to {}", settingsFile);

            // If there's an active in-memory provider for this exact scope, force a reload
            // so it picks up the JSON changes
            String providerKey = groupName + "/" + machineName + "/" + scope;
            ISettingsProvider<?> provider = providers.get(providerKey);
            if (provider != null) {
                provider.reload();
            }

        } catch (Exception e) {
            log.error("Failed to write raw settings to {}", settingsFile, e);
            throw new RuntimeException("Failed to write raw settings", e);
        }
    }
}
