package org.sokybot.http.server.events;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sokybot.commons.topic.Topic;

@Component(service = IEventMediator.class)
public class EventMediatorImpl implements IEventMediator {
    private volatile EventAdmin eventAdmin;
    private volatile IEventBridge eventBridge;

    @Reference
    protected void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    @Reference
    protected void setEventBridge(IEventBridge eventBridge) {
        this.eventBridge = eventBridge;
    }

    @Override
    public <T> void post(Topic topic, T payload) {
        postScoped(null, topic, payload);
    }

    @Override
    public <T> void postScoped(String machineId, Topic topic, T payload) {
        if (topic == null) {
            return;
        }
        if (eventBridge != null) {
            eventBridge.publish(machineId, topic, payload);
        }
        if (eventAdmin != null) {
            Map<String, Object> props = new HashMap<>();
            if (machineId != null && !machineId.isEmpty()) {
                props.put("machineId", machineId);
                props.put("fullName", machineId);
            }
            props.put("event", payload);
            eventAdmin.postEvent(new Event(topic.toEventAdminString(), props));
        }
    }

    @Override
    public <T> IEventBridge.Subscription subscribe(Topic pattern, Consumer<BridgeEvent<T>> callback) {
        if (eventBridge == null) {
            throw new IllegalStateException("Event bridge not available");
        }
        return eventBridge.subscribe(pattern, callback);
    }
}
