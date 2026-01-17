package org.sokybot.machinepages.service;

import java.util.HashMap;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.character.TrainerStuckEvent;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.actuator.training.TrainingSettings;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.settings.api.IProfileManager;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Service for managing training page state and handling training-related events.
 */
public class TrainingService implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(TrainingService.class);
    
    private final String machineFullName;
    private final IMachineContext machineContext;
    private final ISettingsProvider<TrainingSettings> settingsProvider;
    private final IProfileManager profileManager;
    private final Sinks.Many<Map<String, Object>> stateSink = 
        Sinks.many().multicast().onBackpressureBuffer(100);
    
    public TrainingService(
            String machineFullName, 
            IMachineContext machineContext,
            ISettingsRegistry settingsRegistry,
            IProfileManager profileManager) {
        
        this.machineFullName = machineFullName;
        this.machineContext = machineContext;
        this.profileManager = profileManager;
        
        this.settingsProvider = settingsRegistry.getProvider(
            machineContext.getGroupName(),
            machineContext.getMachineName(),
            "training",
            TrainingSettings.class
        );
        
        // Subscribe to settings changes
        this.settingsProvider.subscribe(settings -> emitStateUpdate());
    }
    
    @Override
    public void handleEvent(Event osgiEvent) {
        String fullName = (String) osgiEvent.getProperty("fullName");
        if (fullName == null || !machineFullName.equals(fullName)) {
            return; // Not for this machine
        }
        
        IGameEvent event = (IGameEvent) osgiEvent.getProperty("event");
        if (event instanceof TrainerStuckEvent) {
            handleTrainerStuck((TrainerStuckEvent) event);
        }
        // Note: Training settings changes are handled via action handlers
        // since they're not game events but user configuration changes
    }
    
    private void handleTrainerStuck(TrainerStuckEvent event) {
        logger.debug("Trainer stuck event received");
        // Could update UI to show stuck status
        emitStateUpdate();
    }
    
    public Map<String, Object> handleAction(String action, Map<String, Object> data) {
        Map<String, Object> newState = new HashMap<>();
        
        try {
            switch (action) {
                case "refresh":
                    break;
                case "save":
                    settingsProvider.save();
                    break;
                case "reset":
                    settingsProvider.resetToDefaults();
                    break;
                case "update":
                    settingsProvider.update(settings -> {
                        if (data.containsKey("autoAttack")) settings.setAutoAttack((Boolean) data.get("autoAttack"));
                        if (data.containsKey("doNotAttack")) settings.setDoNotAttack((Boolean) data.get("doNotAttack"));
                        // Add other fields as needed
                    });
                    break;
                default:
                    return Map.of("success", false, "error", "Unknown action: " + action);
            }
            
            newState.putAll(getTrainingData());
            return Map.of("success", true, "state", newState);
            
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    public Flux<Map<String, Object>> streamTraining() {
        return Flux.concat(
            Flux.just(getTrainingData()),
            stateSink.asFlux()
        );
    }
    
    private Map<String, Object> getTrainingData() {
        Map<String, Object> data = new HashMap<>();
        TrainingSettings settings = settingsProvider.get();
        
        data.put("settings", settings);
        data.put("isDirty", settingsProvider.isDirty());
        
        // Add game state (e.g., active area, nearby mobs)
        // For now just returning settings structure
        
        return data;
    }
    
    private void emitStateUpdate() {
        stateSink.tryEmitNext(getTrainingData());
    }
    
    public Map<String, Object> getInitialState() {
        return getTrainingData();
    }
    
    public void shutdown() {
        stateSink.tryEmitComplete();
    }
}
