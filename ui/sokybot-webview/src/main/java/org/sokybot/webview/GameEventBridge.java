package org.sokybot.webview;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.commons.osgi.ServiceHandle;
import org.sokybot.commons.topic.Topic;
import org.sokybot.http.server.events.IEventBridge;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.Map;

@Component(
    immediate = true,
    service = { EventHandler.class, GameEventBridge.class },
    property = {
        EventConstants.EVENT_TOPIC + "=sokybot/game/*",
        EventConstants.EVENT_TOPIC + "=sokybot/network/*"
    }
)
public class GameEventBridge implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(GameEventBridge.class);
    
    // Replay the last 50 events to new subscribers to give context
    private final Sinks.Many<Map<String, Object>> eventSink = Sinks.many().replay().limit(50);
    private final ServiceHandle<IEventBridge> eventBridge = ServiceHandle.create();

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, unbind = "unsetEventBridge")
    protected void setEventBridge(IEventBridge eventBridge) {
        this.eventBridge.bind(eventBridge);
    }

    protected void unsetEventBridge(IEventBridge eventBridge) {
        this.eventBridge.unbind(eventBridge);
    }

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
        eventBridge.ifPresent(bridge -> {
            String machineId = null;
            Object fullName = event.getProperty("fullName");
            if (fullName != null) machineId = String.valueOf(fullName);
            if (machineId == null) {
                Object mid = event.getProperty("machineId");
                if (mid != null) machineId = String.valueOf(mid);
            }
            bridge.publish(machineId, Topic.parse(event.getTopic()), props);
        });
    }
    
    public Flux<Map<String, Object>> getEventStream() {
        return eventSink.asFlux();
    }
}
