package org.sokybot.settings.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sokybot.settings.api.IProfileManager;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of profile manager for group-level shareable profiles.
 */
@Slf4j
@Component(service = IProfileManager.class)
public class ProfileManagerImpl implements IProfileManager {

    /** OSGi Event Admin topic: profile JSON written to disk (Epic #16). */
    public static final String TOPIC_PROFILE_SAVED = "org/sokybot/profile/SAVED";

    private static final String DATA_DIR = "sokybot-data";
    private static final String SETTINGS_DIR = "settings";
    private static final String PROFILES_DIR = "profiles";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ISettingsRegistry settingsRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile EventAdmin eventAdmin;

    @Reference
    public void setSettingsRegistry(ISettingsRegistry settingsRegistry) {
        this.settingsRegistry = settingsRegistry;
    }
    
    // Cache of current settings by scope for profile operations
    private final Map<String, Object> currentSettingsCache = new ConcurrentHashMap<>();
    
    @Override
    public void saveProfile(String groupName, String profileName) {
        try {
            String safeGroup = sanitize(groupName);
            String safeProfile = sanitize(profileName);
            
            Path profileDir = Paths.get(DATA_DIR, SETTINGS_DIR, safeGroup, PROFILES_DIR);
            Files.createDirectories(profileDir);
            
            File profileFile = profileDir.resolve(safeProfile + ".json").toFile();
            
            // Collect all current settings from registry
            Map<String, Object> profileData = new ConcurrentHashMap<>();
            // Note: This would need to iterate over all scopes and collect settings
            // For now, store cached data
            profileData.putAll(currentSettingsCache);
            
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(profileFile, profileData);
            log.info("Saved profile '{}' for group '{}'", profileName, groupName);
            postProfileSavedEvent(groupName, profileName);
        } catch (Exception e) {
            log.error("Failed to save profile '{}' for group '{}'", profileName, groupName, e);
            throw new RuntimeException("Failed to save profile", e);
        }
    }
    
    @Override
    public void loadProfile(String groupName, String machineName, String profileName) {
        try {
            String safeGroup = sanitize(groupName);
            String safeProfile = sanitize(profileName);
            
            File profileFile = Paths.get(DATA_DIR, SETTINGS_DIR, safeGroup, PROFILES_DIR, safeProfile + ".json").toFile();
            
            if (!profileFile.exists()) {
                throw new IllegalArgumentException("Profile not found: " + profileName);
            }
            
            Map<String, Object> profileData = objectMapper.readValue(profileFile, 
                new TypeReference<Map<String, Object>>() {});
            
            // Apply profile data to machine settings
            // Note: This would need to iterate over scopes and apply settings via providers
            currentSettingsCache.clear();
            currentSettingsCache.putAll(profileData);
            
            log.info("Loaded profile '{}' for machine '{}/{}'", profileName, groupName, machineName);
            
        } catch (Exception e) {
            log.error("Failed to load profile '{}' for machine '{}/{}'", profileName, groupName, machineName, e);
            throw new RuntimeException("Failed to load profile", e);
        }
    }
    
    @Override
    public List<String> listProfiles(String groupName) {
        try {
            String safeGroup = sanitize(groupName);
            Path profileDir = Paths.get(DATA_DIR, SETTINGS_DIR, safeGroup, PROFILES_DIR);
            
            if (!Files.exists(profileDir)) {
                return new ArrayList<>();
            }
            
            List<String> profiles = new ArrayList<>();
            Files.list(profileDir)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(p -> {
                    String fileName = p.getFileName().toString();
                    profiles.add(fileName.substring(0, fileName.length() - 5)); // Remove .json
                });
            
            return profiles;
            
        } catch (Exception e) {
            log.error("Failed to list profiles for group '{}'", groupName, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public void deleteProfile(String groupName, String profileName) {
        try {
            String safeGroup = sanitize(groupName);
            String safeProfile = sanitize(profileName);
            
            File profileFile = Paths.get(DATA_DIR, SETTINGS_DIR, safeGroup, PROFILES_DIR, safeProfile + ".json").toFile();
            
            if (profileFile.exists()) {
                profileFile.delete();
                log.info("Deleted profile '{}' from group '{}'", profileName, groupName);
            }
            
        } catch (Exception e) {
            log.error("Failed to delete profile '{}' from group '{}'", profileName, groupName, e);
            throw new RuntimeException("Failed to delete profile", e);
        }
    }
    
    @Override
    public String exportProfile(String groupName, String profileName) {
        try {
            String safeGroup = sanitize(groupName);
            String safeProfile = sanitize(profileName);
            
            File profileFile = Paths.get(DATA_DIR, SETTINGS_DIR, safeGroup, PROFILES_DIR, safeProfile + ".json").toFile();
            
            if (!profileFile.exists()) {
                throw new IllegalArgumentException("Profile not found: " + profileName);
            }
            
            return new String(Files.readAllBytes(profileFile.toPath()));
            
        } catch (Exception e) {
            log.error("Failed to export profile '{}' from group '{}'", profileName, groupName, e);
            throw new RuntimeException("Failed to export profile", e);
        }
    }
    
    @Override
    public void importProfile(String groupName, String profileName, String json) {
        try {
            String safeGroup = sanitize(groupName);
            String safeProfile = sanitize(profileName);
            
            Path profileDir = Paths.get(DATA_DIR, SETTINGS_DIR, safeGroup, PROFILES_DIR);
            Files.createDirectories(profileDir);
            
            File profileFile = profileDir.resolve(safeProfile + ".json").toFile();
            Files.write(profileFile.toPath(), json.getBytes());

            log.info("Imported profile '{}' to group '{}'", profileName, groupName);
            postProfileSavedEvent(groupName, profileName);
        } catch (Exception e) {
            log.error("Failed to import profile '{}' to group '{}'", profileName, groupName, e);
            throw new RuntimeException("Failed to import profile", e);
        }
    }

    private void postProfileSavedEvent(String groupName, String profileId) {
        EventAdmin admin = eventAdmin;
        if (admin == null) {
            return;
        }
        try {
            Map<String, Object> props = new HashMap<>();
            props.put("groupName", groupName);
            props.put("profileId", profileId);
            admin.postEvent(new Event(TOPIC_PROFILE_SAVED, props));
        } catch (Exception e) {
            log.warn("Failed to post profile SAVED event for group {} profile {}: {}", groupName, profileId, e.getMessage());
        }
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
