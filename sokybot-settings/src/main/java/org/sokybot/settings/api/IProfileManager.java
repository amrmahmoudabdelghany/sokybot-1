package org.sokybot.settings.api;

import java.util.List;

/**
 * Profile manager for saving/loading named setting presets at group level.
 * Profiles are shareable across all machines within a group.
 */
public interface IProfileManager {
    
    /**
     * Save current settings as a named profile (group-level, shareable across machines).
     * 
     * @param groupName The group name
     * @param profileName The profile name
     */
    void saveProfile(String groupName, String profileName);
    
    /**
     * Load a profile, replacing current settings for a machine.
     * 
     * @param groupName The group name
     * @param machineName The machine name
     * @param profileName The profile name
     */
    void loadProfile(String groupName, String machineName, String profileName);
    
    /**
     * List available profiles for a group (shareable across all machines).
     * 
     * @param groupName The group name
     * @return List of profile names
     */
    List<String> listProfiles(String groupName);
    
    /**
     * Delete a profile.
     * 
     * @param groupName The group name
     * @param profileName The profile name
     */
    void deleteProfile(String groupName, String profileName);
    
    /**
     * Export profile to portable JSON.
     * 
     * @param groupName The group name
     * @param profileName The profile name
     * @return JSON representation of profile
     */
    String exportProfile(String groupName, String profileName);
    
    /**
     * Import profile from JSON.
     * 
     * @param groupName The group name
     * @param profileName The profile name
     * @param json JSON representation
     */
    void importProfile(String groupName, String profileName, String json);
}
