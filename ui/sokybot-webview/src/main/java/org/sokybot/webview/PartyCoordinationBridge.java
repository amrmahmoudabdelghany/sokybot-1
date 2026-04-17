package org.sokybot.webview;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sokybot.commons.osgi.AtomicServiceHandle;
import org.sokybot.http.server.events.IEventBridge;

/**
 * Bridges EventAdmin party coordination topics into IEventBridge so scripts can subscribe.
 */
@Component(
        immediate = true,
        service = { EventHandler.class, PartyCoordinationBridge.class },
        property = {
                EventConstants.EVENT_TOPIC + "=sokybot/party/*"
        }
)
public class PartyCoordinationBridge implements EventHandler {

    private final AtomicServiceHandle<IEventBridge> eventBridge = new AtomicServiceHandle<>();

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, unbind = "unsetEventBridge")
    protected void setEventBridge(IEventBridge eventBridge) {
        this.eventBridge.set(eventBridge);
    }

    protected void unsetEventBridge(IEventBridge eventBridge) {
        this.eventBridge.clear(eventBridge);
    }

    @Override
    public void handleEvent(Event event) {
        if (event == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        for (String key : event.getPropertyNames()) {
            payload.put(key, event.getProperty(key));
        }
        payload.put("topic", event.getTopic());
        String machineId = null;
        Object targetMachineId = payload.get("targetMachineId");
        if (targetMachineId != null) {
            machineId = String.valueOf(targetMachineId);
        }
        final String finalMachineId = machineId;
        eventBridge.ifPresent(bridge -> bridge.publish(finalMachineId, event.getTopic().replace('/', '.'), payload));
    }
}

