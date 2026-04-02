package org.sokybot.webview.handler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.http.server.events.BridgeEvent;
import org.sokybot.http.server.events.IEventBridge;
import org.sokybot.webview.api.IRSocketStreamHandler;
import org.sokybot.webview.api.RSocketRequest;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component(service = { IRSocketStreamHandler.class, EventHandler.class }, property = {
        IRSocketStreamHandler.STREAM_PROPERTY + "=machine.status.stream",
        EventConstants.EVENT_TOPIC + "=sokybot/network/*"
})
public class MachineStatusStreamHandler implements IRSocketStreamHandler, EventHandler {

    private static final Logger log = LoggerFactory.getLogger(MachineStatusStreamHandler.class);
    private final Sinks.Many<Map<String, Object>> sink = Sinks.many().replay().latest();
    private volatile IEventBridge eventBridge;

    @Reference
    protected void setEventBridge(IEventBridge eventBridge) {
        this.eventBridge = eventBridge;
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
        final IEventBridge bridge = this.eventBridge;
        if (bridge == null) {
            return Flux.just(Map.of("type", "SYNC_REQUIRED", "reason", "event_bridge_unavailable"));
        }
        IEventBridge.Subscription sub = bridge.subscribe("sokybot.network.**", this::onBridgeEvent);
        return sink.asFlux()
                .filter(evt -> {
                    if (requestedMachineId == null || requestedMachineId.isEmpty()) {
                        return true;
                    }
                    Object machineId = evt.get("machineId");
                    return requestedMachineId.equals(machineId);
                })
                .map(evt -> (Object) evt)
                .doFinally(signal -> sub.unsubscribe());
    }

    private void onBridgeEvent(BridgeEvent event) {
        if (event == null) return;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("topic", event.getTopic());
        payload.put("timestamp", System.currentTimeMillis());
        if (event.getMachineId() != null) payload.put("machineId", event.getMachineId());
        Object eventPayload = event.getPayload();
        if (eventPayload instanceof Map<?, ?>) {
            Map<?, ?> m = (Map<?, ?>) eventPayload;
            m.forEach((k, v) -> payload.put(String.valueOf(k), v));
        } else if (eventPayload != null) {
            payload.put("payload", eventPayload);
        }
        enrichUxHints(payload);
        Sinks.EmitResult result = sink.tryEmitNext(payload);
        if (result.isFailure()) {
            log.debug("Dropped machine status event due to sink state: {}", result);
        }
    }

    @Override
    public void handleEvent(Event event) {
        if (event == null || this.eventBridge == null) return;
        Map<String, Object> payload = new HashMap<>();
        for (String name : event.getPropertyNames()) {
            payload.put(name, event.getProperty(name));
        }
        payload.put("topic", event.getTopic());
        String machineId = null;
        Object fullName = event.getProperty("fullName");
        if (fullName != null) machineId = String.valueOf(fullName);
        if (machineId == null) {
            Object mid = event.getProperty("machineId");
            if (mid != null) machineId = String.valueOf(mid);
        }
        this.eventBridge.publish(machineId, event.getTopic().replace('/', '.'), payload);
    }

    private void enrichUxHints(Map<String, Object> payload) {
        String loginPhase = asString(payload.get("loginPhase"));
        if (loginPhase == null || loginPhase.isEmpty()) {
            return;
        }

        if (!payload.containsKey("uxCategory")) {
            payload.put("uxCategory", uxCategoryFor(loginPhase));
        }
        if (!payload.containsKey("requiresInput")) {
            payload.put("requiresInput", requiresInputFor(loginPhase));
        }
        if (!payload.containsKey("fatal")) {
            payload.put("fatal", fatalFor(loginPhase));
        }
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String uxCategoryFor(String phase) {
        switch (phase) {
            case "DISCONNECTED":
            case "MISSING_GATEWAY":
            case "CONNECTING_GATEWAY":
                return "CONNECT";
            case "WAITING_FOR_AGENTS":
            case "WAITING_FOR_AGENTS_TIMEOUT":
            case "MISSING_AGENT_SERVER":
            case "SERVER_INSPECTION":
                return "AGENT";
            case "MISSING_CHARACTER_SELECTION":
                return "CHARACTER";
            case "IN_GAME":
            case "LOADING_ENVIRONMENT":
                return "INGAME";
            case "FAILED":
            case "MANUAL_VERIFICATION_REQUIRED":
            case "RETRY_DISABLED":
            case "RETRY_LIMIT_REACHED":
                return "ERROR";
            default:
                return "AUTH";
        }
    }

    private static boolean requiresInputFor(String phase) {
        return "MISSING_GATEWAY".equals(phase)
                || "MISSING_AGENT_SERVER".equals(phase)
                || "MISSING_CREDENTIALS".equals(phase)
                || "MISSING_CHARACTER_SELECTION".equals(phase)
                || "WAITING_FOR_PASSCODE".equals(phase)
                || "WAIT_FOR_CAPTCHA".equals(phase)
                || "MANUAL_VERIFICATION_REQUIRED".equals(phase);
    }

    private static boolean fatalFor(String phase) {
        return "FAILED".equals(phase)
                || "MANUAL_VERIFICATION_REQUIRED".equals(phase)
                || "RETRY_DISABLED".equals(phase)
                || "RETRY_LIMIT_REACHED".equals(phase);
    }
}
