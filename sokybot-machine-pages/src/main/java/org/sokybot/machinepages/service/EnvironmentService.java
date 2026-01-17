package org.sokybot.machinepages.service;

import java.util.HashMap;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.spawn.MonsterSpawnEvent;
import org.sokybot.gameevents.events.spawn.ItemSpawnEvent;
import org.sokybot.gameevents.events.entity.EntityDespawnEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Service for managing environment page state and handling environment-related events.
 */
public class EnvironmentService implements EventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentService.class);
    
    private final String machineFullName;
    private final Sinks.Many<Map<String, Object>> stateSink = 
        Sinks.many().multicast().onBackpressureBuffer(100);
    
    public EnvironmentService(String machineFullName) {
        this.machineFullName = machineFullName;
    }
    
    @Override
    public void handleEvent(Event osgiEvent) {
        String fullName = (String) osgiEvent.getProperty("fullName");
        if (fullName == null || !machineFullName.equals(fullName)) {
            return; // Not for this machine
        }
        
        IGameEvent event = (IGameEvent) osgiEvent.getProperty("event");
        if (event instanceof MonsterSpawnEvent) {
            handleMonsterSpawn((MonsterSpawnEvent) event);
        } else if (event instanceof ItemSpawnEvent) {
            handleItemSpawn((ItemSpawnEvent) event);
        } else if (event instanceof EntityDespawnEvent) {
            handleEntityDespawn((EntityDespawnEvent) event);
        }
    }
    
    private void handleMonsterSpawn(MonsterSpawnEvent event) {
        logger.debug("Monster spawned: {}", event);
        // Could update monster list in UI
        emitStateUpdate();
    }
    
    private void handleItemSpawn(ItemSpawnEvent event) {
        logger.debug("Item spawned: {}", event);
        // Could update item list in UI
        emitStateUpdate();
    }
    
    private void handleEntityDespawn(EntityDespawnEvent event) {
        logger.debug("Entity despawned: {}", event);
        // Could update entity list in UI
        emitStateUpdate();
    }
    
    public Map<String, Object> handleAction(String action, Map<String, Object> data) {
        Map<String, Object> newState = new HashMap<>();
        switch (action) {
            case "refresh":
                newState.putAll(getEnvironmentData());
                return Map.of("success", true, "state", newState);
            case "updateMonsterPreferences":
                // TODO: Implement monster preferences update
                return Map.of("success", false, "error", "Not implemented");
            case "updateAreaFilters":
                // TODO: Implement area filters update
                return Map.of("success", false, "error", "Not implemented");
            default:
                return Map.of("success", false, "error", "Unknown action: " + action);
        }
    }
    
    public Flux<Map<String, Object>> streamEnvironment() {
        // Return initial state + updates
        return Flux.concat(
            Flux.just(getEnvironmentData()),
            stateSink.asFlux()
        );
    }
    
    private Map<String, Object> getEnvironmentData() {
        Map<String, Object> data = new HashMap<>();
        // Placeholder - environment data will be populated from settings
        data.put("monsterPreferences", new HashMap<>());
        data.put("areaFilters", new HashMap<>());
        return data;
    }
    
    private void emitStateUpdate() {
        stateSink.tryEmitNext(getEnvironmentData());
    }
    
    public Map<String, Object> getInitialState() {
        return getEnvironmentData();
    }
    
    public void shutdown() {
        stateSink.tryEmitComplete();
    }
}
