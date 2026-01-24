package org.sokybot.webview.handler;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.webview.GameEventBridge;
import org.sokybot.webview.api.IRSocketStreamHandler;
import org.sokybot.webview.api.RSocketRequest;

import reactor.core.publisher.Flux;

/**
 * Stream handler for game events.
 * 
 * Stream: game.events
 * Provides a continuous stream of game events to the frontend.
 */
@Component(
    service = IRSocketStreamHandler.class,
    property = IRSocketStreamHandler.STREAM_PROPERTY + "=game.events"
)
public class GameEventsStreamHandler implements IRSocketStreamHandler {
    
    private GameEventBridge eventBridge;
    
    @Reference
    protected void setEventBridge(GameEventBridge eventBridge) {
        this.eventBridge = eventBridge;
    }
    
    @Override
    public String getStreamName() {
        return "game.events";
    }
    
    @Override
    public String getDescription() {
        return "Stream of game events (character updates, combat, etc.)";
    }
    
    @Override
    public Flux<Object> handleStream(RSocketRequest request) {
        return eventBridge.getEventStream().map(event -> event);
    }
}
