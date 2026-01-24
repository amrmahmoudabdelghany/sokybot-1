package org.sokybot.settings.api;

import java.util.function.Consumer;

/**
 * Provider interface for actuator-scoped settings.
 * Provides explicit save/reload, dirty tracking, and reactive subscriptions.
 * 
 * @param <T> The settings type
 */
public interface ISettingsProvider<T> {
    
    /**
     * Get current settings snapshot (never null).
     * 
     * @return Current settings instance
     */
    T get();
    
    /**
     * Apply mutations to settings (changes NOT persisted automatically).
     * 
     * @param mutator Consumer that modifies the settings
     */
    void update(Consumer<T> mutator);
    
    /**
     * Explicitly persist current state to disk.
     */
    void save();
    
    /**
     * Reload from disk, discarding in-memory changes.
     */
    void reload();
    
    /**
     * Reset settings to factory defaults.
     */
    void resetToDefaults();
    
    /**
     * Check if there are unsaved changes.
     * 
     * @return true if settings have been modified since last save
     */
    boolean isDirty();
    
    /**
     * Subscribe to setting changes (triggered on update).
     * 
     * @param listener Consumer to call on changes
     */
    void subscribe(Consumer<T> listener);
}
