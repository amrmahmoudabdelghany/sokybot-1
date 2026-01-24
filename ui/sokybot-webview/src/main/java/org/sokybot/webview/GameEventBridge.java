package org.sokybot.webview;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;

@Component(
    immediate = true,
    service = { EventHandler.class, GameEventBridge.class },
    property = {
        EventConstants.EVENT_TOPIC + "=sokybot/game/*"
    }
)
public class GameEventBridge implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(GameEventBridge.class);
    
    // Replay the last 50 events to new subscribers to give context
    private final Sinks.Many<Map<String, Object>> eventSink = Sinks.many().replay().limit(50);

    @Override
    public void handleEvent(Event event) {
        Map<String, Object> props = new HashMap<>();
        for (String key : event.getPropertyNames()) {
            // Filter out OSGi specific properties if needed, or just include all
            props.put(key, event.getProperty(key));
        }
        
        // We assume "event" property contains the IGameEvent object. 
        // Jackson will try to serialize it. 
        
        Sinks.EmitResult result = eventSink.tryEmitNext(props);
        if (result.isFailure()) {
             log.warn("Failed to emit event: {}", result);
        }
    }
    
    public Flux<Map<String, Object>> getEventStream() {
        return eventSink.asFlux();
    }
}
