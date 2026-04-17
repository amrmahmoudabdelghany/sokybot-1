package org.sokybot.webview;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
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

    private volatile IEventBridge eventBridge;

    @Reference
    protected void setEventBridge(IEventBridge eventBridge) {
        this.eventBridge = eventBridge;
    }

    @Override
    public void handleEvent(Event event) {
        if (event == null || eventBridge == null) {
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
        eventBridge.publish(machineId, event.getTopic().replace('/', '.'), payload);
    }
}

