package org.sokybot.settings;

public interface ISettingsManager {

    /**
     * Loads settings for a specific scope (bundle/plugin).
     * 
     * @param groupName The name of the server/group.
     * @param machineName The name of the bot/machine.
     * @param scope The scope identifier (e.g., "core", "auto-potion").
     * @param type The class of the settings object to return.
     * @return The loaded settings object, or a new instance if not found.
     */
    <T> T loadSettings(String groupName, String machineName, String scope, Class<T> type);

    /**
     * Saves settings for a specific scope.
     * 
     * @param groupName The name of the server/group.
     * @param machineName The name of the bot/machine.
     * @param scope The scope identifier.
     * @param settingsObject The object to save.
     */
    void saveSettings(String groupName, String machineName, String scope, Object settingsObject);

    /**
     * Checks if a persistent configuration exists for this scope.
     */
    boolean exists(String groupName, String machineName, String scope);

    /**
     * Deletes the configuration file for this scope.
     */
    void delete(String groupName, String machineName, String scope);
}
