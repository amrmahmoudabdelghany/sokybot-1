package org.sokybot.webview.handler;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.http.server.events.BridgeEvent;
import org.sokybot.http.server.events.IEventBridge;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.IRSocketStreamHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * RSocket handler for event bridge operations.
 * 
 * Request-Response Methods:
 * - events.status: Get event bridge status
 * 
 * Stream Methods:
 * - events.stream: Subscribe to real-time events
 */
@Component(service = { IRSocketHandler.class, IRSocketStreamHandler.class }, property = {
        IRSocketHandler.METHOD_PROPERTY + "=events.status",
        IRSocketStreamHandler.STREAM_PROPERTY + "=events.stream"
})
public class EventBridgeHandler implements IRSocketHandler, IRSocketStreamHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private volatile IEventBridge eventBridge;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setEventBridge(IEventBridge bridge) {
        this.eventBridge = bridge;
    }

    protected void unsetEventBridge(IEventBridge bridge) {
        this.eventBridge = null;
    }

    // ===== IRSocketHandler =====

    @Override
    public String[] getMethods() {
        return new String[] { "events.status" };
    }

    @Override
    public String getDescription() {
        return "Real-time event bridge";
    }

    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        String method = request.getMethod();

        if ("events.status".equals(method)) {
            return handleStatus(request);
        }

        return Mono.just(RSocketResponse.methodNotFound(method));
    }

    private Mono<RSocketResponse> handleStatus(RSocketRequest request) {
        if (eventBridge == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Event bridge not available"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("subscriberCount", eventBridge.getSubscriberCount());
        response.put("eventCount", eventBridge.getEventCount());
        response.put("available", true);

        return Mono.just(RSocketResponse.success(response));
    }

    // ===== IRSocketStreamHandler =====

    @Override
    public String getStreamName() {
        return "events.stream";
    }

    @Override
    public Flux<Object> handleStream(RSocketRequest request) {
        if (eventBridge == null) {
            return Flux.error(new IllegalStateException("Event bridge not available"));
        }

        String pattern = request.getString("pattern");
        if (pattern == null || pattern.isEmpty()) {
            pattern = "**"; // Subscribe to all events by default
        }

        String machineId = request.getString("machineId");

        final String finalPattern = pattern;

        // Create a sink for emitting events
        Sinks.Many<Object> sink = Sinks.many().multicast().onBackpressureBuffer();

        // Store subscription reference for cleanup
        AtomicReference<IEventBridge.Subscription> subRef = new AtomicReference<>();

        // Subscribe to events
        IEventBridge.Subscription subscription = eventBridge.subscribe(
                machineId,
                finalPattern,
                event -> {
                    Map<String, Object> eventMap = eventToMap(event);
                    sink.tryEmitNext(eventMap);
                });
        subRef.set(subscription);

        // Return flux that cleans up on cancel
        return sink.asFlux()
                .doOnCancel(() -> {
                    IEventBridge.Subscription sub = subRef.get();
                    if (sub != null) {
                        sub.unsubscribe();
                    }
                })
                .doOnTerminate(() -> {
                    IEventBridge.Subscription sub = subRef.get();
                    if (sub != null) {
                        sub.unsubscribe();
                    }
                });
    }

    private Map<String, Object> eventToMap(BridgeEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("topic", event.getTopic());
        map.put("timestamp", TIMESTAMP_FORMATTER.format(event.getTimestamp()));

        if (event.getMachineId() != null) {
            map.put("machineId", event.getMachineId());
        }

        map.put("payload", event.getPayload());

        if (event.getMetadata() != null) {
            map.put("metadata", event.getMetadata());
        }

        return map;
    }
}
