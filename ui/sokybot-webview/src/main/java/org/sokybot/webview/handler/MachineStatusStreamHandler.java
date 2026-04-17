package org.sokybot.webview.handler;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.commons.osgi.AtomicServiceHandle;
import org.sokybot.http.server.events.IEventBridge;
import org.sokybot.webview.api.IRSocketStreamHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.status.MachineStatusHubRegistry;

import reactor.core.publisher.Flux;

@Component(service = { IRSocketStreamHandler.class, EventHandler.class }, property = {
        IRSocketStreamHandler.STREAM_PROPERTY + "=machine.status.stream",
        EventConstants.EVENT_TOPIC + "=sokybot/network/*"
})
public class MachineStatusStreamHandler implements IRSocketStreamHandler, EventHandler {

    private static final Logger log = LoggerFactory.getLogger(MachineStatusStreamHandler.class);

    private final AtomicServiceHandle<IEventBridge> eventBridge = new AtomicServiceHandle<>();
    private volatile MachineStatusHubRegistry hubRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, unbind = "unsetEventBridge")
    protected void setEventBridge(IEventBridge eventBridge) {
        this.eventBridge.set(eventBridge);
    }

    protected void unsetEventBridge(IEventBridge eventBridge) {
        this.eventBridge.clear(eventBridge);
    }

    @Reference
    protected void setHubRegistry(MachineStatusHubRegistry hubRegistry) {
        this.hubRegistry = hubRegistry;
    }

    @Override
    public String getStreamName() {
        return "machine.status.stream";
    }

    @Override
    public String getDescription() {
        return "Machine network/auth status transitions from IConnectionListener callbacks";
    }

    @Override
    public Flux<Object> handleStream(RSocketRequest request) {
        final String requestedMachineId = request.getString("machineId");
        final IEventBridge bridge = this.eventBridge.tryGet().orElse(null);
        final MachineStatusHubRegistry registry = this.hubRegistry;

        if (bridge == null) {
            return Flux.just(Map.of("type", "SYNC_REQUIRED", "reason", "event_bridge_unavailable"));
        }
        if (registry == null) {
            return Flux.just(Map.of("type", "SYNC_REQUIRED", "reason", "status_hub_unavailable"));
        }

        if (requestedMachineId == null || requestedMachineId.isEmpty()) {
            Flux<Map<String, Object>> heartbeats = Flux.interval(Duration.ofSeconds(10))
                    .map(t -> Map.<String, Object>of(
                            "type", "heartbeat",
                            "timestamp", System.currentTimeMillis()));
            return Flux.merge(
                    registry.wildcardStatusFlux().onBackpressureLatest(),
                    heartbeats)
                    .map(m -> (Object) m)
                    .doOnError(e -> log.warn("Wildcard machine status stream error", e));
        }

        return registry.machineStatusFlux(requestedMachineId)
                .onBackpressureLatest()
                .switchIfEmpty(Flux.just(Map.of(
                        "type", "SYNC_REQUIRED",
                        "reason", "machine_not_found",
                        "machineId", requestedMachineId)))
                .map(m -> (Object) m)
                .doOnError(e -> log.warn("Machine status stream error for {}", requestedMachineId, e));
    }

    @Override
    public void handleEvent(Event event) {
        if (event == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        for (String name : event.getPropertyNames()) {
            payload.put(name, event.getProperty(name));
        }
        payload.put("topic", event.getTopic());
        String machineId = null;
        Object fullName = event.getProperty("fullName");
        if (fullName != null) {
            machineId = String.valueOf(fullName);
        }
        if (machineId == null) {
            Object mid = event.getProperty("machineId");
            if (mid != null) {
                machineId = String.valueOf(mid);
            }
        }
        final String finalMachineId = machineId;
        eventBridge.ifPresent(bridge -> bridge.publish(finalMachineId, event.getTopic().replace('/', '.'), payload));
    }
}
