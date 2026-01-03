package org.sokybot.service;

public interface ISettingsService {

    /**
     * Retrieves the settings object for a specific plugin.
     * @param pluginId Unique identifier for the plugin.
     * @param type The class type of the settings POJO.
     * @return The settings object, or a new instance if not found.
     */
    <T> T getPluginSettings(String pluginId, Class<T> type);

    /**
     * Saves the settings object for a specific plugin.
     * @param pluginId Unique identifier for the plugin.
     * @param settings The settings object to save.
     */
    void savePluginSettings(String pluginId, Object settings);
}
