package org.sokybot.settings.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.security.Encrypted;
import org.sokybot.settings.security.ICredentialEncryptor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Settings provider implementation with dirty tracking, encryption, and JSON persistence.
 */
@Slf4j
public class SettingsProviderImpl<T> implements ISettingsProvider<T> {
    
    private final Class<T> type;
    private final Supplier<T> defaultsFactory;
    private final Path settingsFile;
    private final ObjectMapper objectMapper;
    private final ICredentialEncryptor credentialEncryptor;
    
    private T currentSettings;
    private T savedSettings;
    private boolean dirty = false;
    
    private final List<Consumer<T>> subscribers = new ArrayList<>();
    
    public SettingsProviderImpl(
            Class<T> type,
            Supplier<T> defaultsFactory,
            Path settingsFile,
            ObjectMapper objectMapper,
            ICredentialEncryptor credentialEncryptor) {
        
        this.type = type;
        this.defaultsFactory = defaultsFactory;
        this.settingsFile = settingsFile;
        this.objectMapper = objectMapper;
        this.credentialEncryptor = credentialEncryptor;
        
        // Load or create default settings
        this.currentSettings = load();
        this.savedSettings = cloneSettings(currentSettings);
    }
    
    @Override
    public synchronized T get() {
        return currentSettings;
    }
    
    @Override
    public synchronized void update(Consumer<T> mutator) {
        mutator.accept(currentSettings);
        dirty = true;
        notifySubscribers();
    }
    
    @Override
    public synchronized void save() {
        try {
            // Encrypt sensitive fields before saving
            T toSave = cloneSettings(currentSettings);
            encryptFields(toSave);
            
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(settingsFile.toFile(), toSave);
            
            savedSettings = cloneSettings(currentSettings);
            dirty = false;
            
            log.info("Saved settings to {}", settingsFile);
            
        } catch (Exception e) {
            log.error("Failed to save settings to {}", settingsFile, e);
            throw new RuntimeException("Failed to save settings", e);
        }
    }
    
    @Override
    public synchronized void reload() {
        currentSettings = load();
        savedSettings = cloneSettings(currentSettings);
        dirty = false;
        notifySubscribers();
        log.info("Reloaded settings from {}", settingsFile);
    }
    
    @Override
    public synchronized void resetToDefaults() {
        currentSettings = defaultsFactory.get();
        dirty = true;
        notifySubscribers();
        log.info("Reset settings to defaults");
    }
    
    @Override
    public synchronized boolean isDirty() {
        return dirty;
    }
    
    @Override
    public synchronized void subscribe(Consumer<T> listener) {
        subscribers.add(listener);
    }
    
    private T load() {
        if (!Files.exists(settingsFile)) {
            log.debug("Settings file does not exist, creating defaults: {}", settingsFile);
            return defaultsFactory.get();
        }
        
        try {
            T settings = objectMapper.readValue(settingsFile.toFile(), type);
            decryptFields(settings);
            return settings;
            
        } catch (IOException e) {
            log.error("Failed to load settings from {}, using defaults", settingsFile, e);
            return defaultsFactory.get();
        }
    }
    
    private void encryptFields(T settings) {
        if (!credentialEncryptor.isUnlocked()) {
            log.warn("Credential encryptor is locked, skipping encryption");
            return;
        }
        
        try {
            for (Field field : settings.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Encrypted.class)) {
                    field.setAccessible(true);
                    Object value = field.get(settings);
                    if (value instanceof String) {
                        String encrypted = credentialEncryptor.encrypt((String) value);
                        field.set(settings, encrypted);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to encrypt fields", e);
        }
    }
    
    private void decryptFields(T settings) {
        if (!credentialEncryptor.isUnlocked()) {
            log.warn("Credential encryptor is locked, skipping decryption");
            return;
        }
        
        try {
            for (Field field : settings.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Encrypted.class)) {
                    field.setAccessible(true);
                    Object value = field.get(settings);
                    if (value instanceof String) {
                        String decrypted = credentialEncryptor.decrypt((String) value);
                        field.set(settings, decrypted);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to decrypt fields", e);
        }
    }
    
    private T cloneSettings(T settings) {
        try {
            String json = objectMapper.writeValueAsString(settings);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.error("Failed to clone settings", e);
            return defaultsFactory.get();
        }
    }
    
    private void notifySubscribers() {
        for (Consumer<T> subscriber : subscribers) {
            try {
                subscriber.accept(currentSettings);
            } catch (Exception e) {
                log.error("Error notifying subscriber", e);
            }
        }
    }
}
