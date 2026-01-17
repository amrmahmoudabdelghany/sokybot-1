package org.sokybot.machinepages.service;

import java.util.HashMap;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.character.TrainerStuckEvent;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.settings.Settings;
import org.sokybot.settings.TrainingArea;
import org.sokybot.settings.TrainingAreaSettings;
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
    private final Sinks.Many<Map<String, Object>> stateSink = 
        Sinks.many().multicast().onBackpressureBuffer(100);
    
    public TrainingService(String machineFullName, IMachineContext machineContext) {
        this.machineFullName = machineFullName;
        this.machineContext = machineContext;
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
        Settings settings = machineContext != null ? machineContext.getSettings() : null;
        
        if (settings == null) {
            return Map.of("success", false, "error", "Settings not available");
        }
        
        switch (action) {
            case "refresh":
                newState.putAll(getTrainingData(settings));
                return Map.of("success", true, "state", newState);
            case "setAutoAttack":
                boolean autoAttack = (Boolean) data.getOrDefault("value", false);
                settings.setAutoAttack(autoAttack);
                newState.put("autoAttack", autoAttack);
                emitStateUpdate();
                return Map.of("success", true, "state", newState);
            case "setAutoLogin":
                boolean autoLogin = (Boolean) data.getOrDefault("value", false);
                settings.setAutoLogin(autoLogin);
                newState.put("autoLogin", autoLogin);
                emitStateUpdate();
                return Map.of("success", true, "state", newState);
            case "setActiveArea":
                // TODO: Implement active area setting
                return Map.of("success", false, "error", "Not implemented");
            default:
                return Map.of("success", false, "error", "Unknown action: " + action);
        }
    }
    
    public Flux<Map<String, Object>> streamTraining() {
        // Return initial state + updates
        Settings settings = machineContext != null ? machineContext.getSettings() : null;
        return Flux.concat(
            Flux.just(getTrainingData(settings)),
            stateSink.asFlux()
        );
    }
    
    private Map<String, Object> getTrainingData(Settings settings) {
        Map<String, Object> data = new HashMap<>();
        if (settings != null) {
            data.put("autoAttack", settings.isAutoAttack());
            data.put("autoLogin", settings.isAutoLogin());
            
            TrainingAreaSettings areaSettings = settings.getTrainingAreaSettings();
            if (areaSettings != null) {
                TrainingArea active = areaSettings.getActiveAreaInstance();
                if (active != null) {
                    Map<String, Object> area = new HashMap<>();
                    area.put("name", active.getName());
                    area.put("x", active.getAreaX());
                    area.put("y", active.getAreaY());
                    area.put("r", active.getAreaR());
                    data.put("activeArea", area);
                } else {
                    data.put("activeArea", null);
                }
            } else {
                data.put("activeArea", null);
            }
        } else {
            data.put("autoAttack", false);
            data.put("autoLogin", false);
            data.put("activeArea", null);
        }
        return data;
    }
    
    private void emitStateUpdate() {
        Settings settings = machineContext != null ? machineContext.getSettings() : null;
        stateSink.tryEmitNext(getTrainingData(settings));
    }
    
    public Map<String, Object> getInitialState() {
        Settings settings = machineContext != null ? machineContext.getSettings() : null;
        return getTrainingData(settings);
    }
    
    public void shutdown() {
        stateSink.tryEmitComplete();
    }
}
