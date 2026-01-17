package org.sokybot.machine.listener;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gamemodel.IGameModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GameModelStateListener {

    @Autowired
    private IGameModel model;

    @EventListener
    public void onGameEvent(IGameEvent event) {
        // Generic handler or specific subtypes
        // For demonstration, logging or basic updates
    }
    
    // Add specific event listeners here
    // e.g. @EventListener public void onSpawn(ObjectSpawnEvent event) { ... }
}
