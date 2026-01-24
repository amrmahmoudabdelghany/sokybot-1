package org.sokybot.machinepages.service;

import java.util.HashMap;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sokybot.actuator.training.TrainingSettings;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Service for managing navigation page state (Map, Waypoints, Town Loop).
 */
public class NavigationService implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(NavigationService.class);
    
    private final String machineFullName;
    private final IMachineContext machineContext;
    private final ISettingsProvider<TrainingSettings> settingsProvider;
    private final Sinks.Many<Map<String, Object>> stateSink = 
        Sinks.many().multicast().onBackpressureBuffer(100);
    
    public NavigationService(
            String machineFullName, 
            IMachineContext machineContext,
            ISettingsRegistry settingsRegistry) {
        
        this.machineFullName = machineFullName;
        this.machineContext = machineContext;
        
        // Navigation settings are part of TrainingSettings
        this.settingsProvider = settingsRegistry.getProvider(
            machineContext.getGroupName(),
            machineContext.getMachineName(),
            "training",
            TrainingSettings.class
        );
        
        this.settingsProvider.subscribe(settings -> emitStateUpdate());
    }
    
    @Override
    public void handleEvent(Event osgiEvent) {
        String fullName = (String) osgiEvent.getProperty("fullName");
        if (fullName == null || !machineFullName.equals(fullName)) {
            return;
        }
        
        // Handle movement events
        String topic = osgiEvent.getTopic();
        if (topic.contains("PositionUpdate") || topic.contains("MapChanged")) {
            emitStateUpdate();
        }
    }
    
    public Map<String, Object> handleAction(String action, Map<String, Object> data) {
        try {
            switch (action) {
                case "refresh":
                    break;
                case "save":
                    settingsProvider.save();
                    break;
                case "update":
                    settingsProvider.update(settings -> {
                        if (data.containsKey("loopInTown")) 
                            settings.setLoopInTown((Boolean) data.get("loopInTown"));
                        if (data.containsKey("scriptPath")) 
                            settings.setScriptPath((String) data.get("scriptPath"));
                    });
                    break;
                default:
                    return Map.of("success", false, "error", "Unknown action: " + action);
            }
            
            return Map.of("success", true, "state", getState());
            
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }
    
    public Flux<Map<String, Object>> streamNavigation() {
        return Flux.concat(
            Flux.just(getState()),
            stateSink.asFlux()
        );
    }
    
    private Map<String, Object> getState() {
        Map<String, Object> data = new HashMap<>();
        
        // Settings
        TrainingSettings settings = settingsProvider.get();
        data.put("settings", settings);
        
        // Game State (Position)
        try {
            ITrainer trainer = machineContext.getGameModel().getTrainer();
            if (trainer != null) {
                Map<String, Object> pos = new HashMap<>();
                pos.put("x", trainer.getX());
                pos.put("y", trainer.getY());
                // pos.put("mapId", trainer.getMapId()); // Assuming mapId exists
                data.put("position", pos);
            }
        } catch (Exception e) {
            // Ignore
        }
        
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
