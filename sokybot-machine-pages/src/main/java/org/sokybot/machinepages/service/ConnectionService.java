package org.sokybot.machinepages.service;

import java.util.HashMap;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sokybot.actuator.login.LoginSettings;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.settings.api.IProfileManager;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.settings.security.ICredentialEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Service for managing connection page state (Login settings + connectivity).
 */
public class ConnectionService implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);
    
    private final String machineFullName;
    private final IMachineContext machineContext;
    private final ISettingsProvider<LoginSettings> settingsProvider;
    private final IProfileManager profileManager;
    private final ICredentialEncryptor credentialEncryptor;
    
    private final Sinks.Many<Map<String, Object>> stateSink = 
        Sinks.many().multicast().onBackpressureBuffer(100);
    
    public ConnectionService(
            String machineFullName, 
            IMachineContext machineContext,
            ISettingsRegistry settingsRegistry,
            IProfileManager profileManager,
            ICredentialEncryptor credentialEncryptor) {
        
        this.machineFullName = machineFullName;
        this.machineContext = machineContext;
        this.profileManager = profileManager;
        this.credentialEncryptor = credentialEncryptor;
        
        this.settingsProvider = settingsRegistry.getProvider(
            machineContext.getGroupName(),
            machineContext.getMachineName(),
            "login",
            LoginSettings.class
        );
        
        // Subscribe to settings changes
        this.settingsProvider.subscribe(settings -> emitStateUpdate());
    }
    
    @Override
    public void handleEvent(Event osgiEvent) {
        String fullName = (String) osgiEvent.getProperty("fullName");
        if (fullName == null || !machineFullName.equals(fullName)) {
            return;
        }
        
        // Handle connection events if any
        String topic = osgiEvent.getTopic();
        if (topic.endsWith("Connected") || topic.endsWith("Disconnected")) {
            emitStateUpdate();
        }
    }
    
    public Map<String, Object> handleAction(String action, Map<String, Object> data) {
        Map<String, Object> newState = new HashMap<>();
        
        try {
            switch (action) {
                case "refresh":
                    break; // Just return state
                    
                case "save":
                    settingsProvider.save();
                    break;
                    
                case "reset":
                    settingsProvider.resetToDefaults();
                    break;
                    
                case "update":
                    updateSettings(data);
                    break;
                    
                case "unlock":
                    String passphrase = (String) data.get("passphrase");
                    credentialEncryptor.unlock(passphrase);
                    settingsProvider.reload(); // Reload to decrypt fields
                    break;
                    
                case "lock":
                    credentialEncryptor.lock();
                    stateSink.tryEmitNext(getState());
                    break;
                    
                case "saveProfile":
                    String saveName = (String) data.get("profileName");
                    if (saveName != null && !saveName.isEmpty()) {
                        profileManager.saveProfile(machineContext.getGroupName(), saveName);
                    }
                    break;
                    
                case "loadProfile":
                    String loadName = (String) data.get("profileName");
                    if (loadName != null && !loadName.isEmpty()) {
                        profileManager.loadProfile(machineContext.getGroupName(), machineContext.getMachineName(), loadName);
                        // Reload provider to pick up changes
                        settingsProvider.reload();
                    }
                    break;
                    
                case "deleteProfile":
                    String delName = (String) data.get("profileName");
                    if (delName != null && !delName.isEmpty()) {
                        profileManager.deleteProfile(machineContext.getGroupName(), delName);
                    }
                    break;
                    
                default:
                    return Map.of("success", false, "error", "Unknown action: " + action);
            }
            
            newState.putAll(getState());
            newState.put("success", true);
            return newState;
            
        } catch (Exception e) {
            logger.error("Error handling action: " + action, e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    private void updateSettings(Map<String, Object> data) {
        settingsProvider.update(settings -> {
            if (data.containsKey("username")) settings.setUsername((String) data.get("username"));
            if (data.containsKey("password")) settings.setPassword((String) data.get("password"));
            if (data.containsKey("targetGateway")) settings.setTargetGateway((String) data.get("targetGateway"));
            if (data.containsKey("autoLogin")) settings.setAutoLogin((Boolean) data.get("autoLogin"));
            if (data.containsKey("targetAgent")) settings.setTargetAgent((String) data.get("targetAgent"));
        });
    }
    
    public Flux<Map<String, Object>> streamState() {
        return Flux.concat(
            Flux.just(getState()),
            stateSink.asFlux()
        );
    }
    
    private Map<String, Object> getState() {
        Map<String, Object> data = new HashMap<>();
        
        // Settings data
        LoginSettings settings = settingsProvider.get();
        data.put("settings", settings);
        data.put("isDirty", settingsProvider.isDirty());
        
        // Security state
        boolean unlocked = credentialEncryptor.isUnlocked();
        data.put("isUnlocked", unlocked);
        
        // Hide password if locked (though settings provider might return encrypted string)
        if (!unlocked) {
            // If locked, settings might contain ciphertext. We can mask it for UI.
            // Or let UI handle it.
        }
        
        // Connection state
        boolean connected = machineContext.getDispatcher().isConnected();
        data.put("connected", connected);
        
        // Profiles
        data.put("profiles", profileManager.listProfiles(machineContext.getGroupName()));
        
        return data;
    }
    
    private void emitStateUpdate() {
        stateSink.tryEmitNext(getState());
    }
    
    public Map<String, Object> getInitialState() {
        return getState();
    }
    
    public void shutdown() {
        stateSink.tryEmitComplete();
    }
}
