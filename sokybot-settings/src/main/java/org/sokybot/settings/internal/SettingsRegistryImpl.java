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
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> ISettingsProvider<T> getProvider(String groupName, String machineName, String scope, Class<T> type) {
        String key = groupName + "/" + machineName + "/" + scope;
        
        return (ISettingsProvider<T>) providers.computeIfAbsent(key, k -> {
           ScopeRegistration<?> registration = registrations.get(scope);
            if (registration == null) {
                throw new IllegalArgumentException("Scope not registered: " + scope);
            }
            
            if (!registration.type.equals(type)) {
                throw new IllegalArgumentException("Type mismatch for scope " + scope);
            }
            
            Path settingsFile = getSettingsFile(groupName, machineName, scope);
            
            return new SettingsProviderImpl<>(
                (Class<T>) registration.type,
                (Supplier<T>) registration.defaultsFactory,
                settingsFile,
                objectMapper,
                credentialEncryptor
            );
        });
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
    
    private static class ScopeRegistration<T> {
        final Class<T> type;
        final Supplier<T> defaultsFactory;
        
        ScopeRegistration(Class<T> type, Supplier<T> defaultsFactory) {
            this.type = type;
            this.defaultsFactory = defaultsFactory;
        }
    }
}
