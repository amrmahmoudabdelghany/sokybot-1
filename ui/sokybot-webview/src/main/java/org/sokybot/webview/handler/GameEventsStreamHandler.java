package org.sokybot.webview.handler;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.http.server.events.BridgeEvent;
import org.sokybot.http.server.events.IEventBridge;
import org.sokybot.webview.api.IRSocketStreamHandler;
import org.sokybot.webview.api.RSocketRequest;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

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

    private final Sinks.Many<Object> sink = Sinks.many().replay().latest();
    private IEventBridge eventBridge;

    @Reference
    protected void setEventBridge(IEventBridge eventBridge) {
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
        if (eventBridge == null) {
            return Flux.just(java.util.Map.of("type", "SYNC_REQUIRED", "reason", "event_bridge_unavailable"));
        }
        IEventBridge.Subscription subscription = eventBridge.subscribe("sokybot.game.**", this::onEvent);
        return sink.asFlux().doFinally(signal -> subscription.unsubscribe());
    }

    private void onEvent(BridgeEvent event) {
        Object payload = event.getPayload();
        if (payload != null) {
            sink.tryEmitNext(payload);
        }
    }
}
