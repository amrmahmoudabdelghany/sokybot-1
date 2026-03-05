package org.sokybot.settings.api;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Central registry for actuators to register their settings types.
 * Manages settings scopes and provides access to settings providers.
 */
public interface ISettingsRegistry {

    /**
     * Register a settings type for a scope.
     * 
     * @param scope           Unique scope ID (e.g., "login", "training", "combat")
     * @param type            Settings class (must have no-arg constructor)
     * @param defaultsFactory Factory to create default settings instance
     * @param <T>             Settings type
     */
    <T> void register(String scope, Class<T> type, Supplier<T> defaultsFactory);

    /**
     * Get a settings provider for a scope + machine.
     * 
     * @param groupName   The group name
     * @param machineName The machine name
     * @param scope       The settings scope
     * @param type        Settings class
     * @param <T>         Settings type
     * @return Settings provider for this scope/machine
     */
    <T> ISettingsProvider<T> getProvider(String groupName, String machineName, String scope, Class<T> type);

    /**
     * List all registered scopes.
     * 
     * @return Set of registered scope IDs
     */
    Set<String> getRegisteredScopes();

    /**
     * Dynamically write settings to disk without requiring a registered generic
     * type.
     * This is useful for initialization/UI components that don't depend on the
     * explicit model class.
     * 
     * @param groupName   The group name
     * @param machineName The machine name
     * @param scope       The settings scope (e.g. "login", "fishing")
     * @param data        The raw key-value mapping of settings
     */
    void writeRawSettings(String groupName, String machineName, String scope, java.util.Map<String, Object> data);
}
