package org.sokybot.webview.handler;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Sinks.Many<Map<String, Object>> sink = Sinks.many().multicast().onBackpressureBuffer();

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
        return sink.asFlux()
                .filter(evt -> {
                    if (requestedMachineId == null || requestedMachineId.isEmpty()) {
                        return true;
                    }
                    Object machineId = evt.get("machineId");
                    return requestedMachineId.equals(machineId);
                })
                .map(evt -> evt);
    }

    @Override
    public void handleEvent(Event event) {
        if (event == null) {
            return;
        }

        String topic = event.getTopic();
        Map<String, Object> payload = new HashMap<>();

        if (topic != null && topic.startsWith("sokybot/game/") && topic.endsWith("/AgentListEvent")) {
            // Avoid putting the raw IGameEvent on the stream payload (JSON serialization).
            String[] segments = topic.split("/");
            if (segments.length >= 4) {
                payload.put("machineId", segments[2]);
            }
            payload.put("transition", "AgentListReceived");
            payload.put("topic", topic);
            payload.put("timestamp", System.currentTimeMillis());
            Sinks.EmitResult result = sink.tryEmitNext(payload);
            if (result.isFailure()) {
                log.debug("Dropped machine status event due to sink state: {}", result);
            }
            return;
        }

        for (String name : event.getPropertyNames()) {
            payload.put(name, event.getProperty(name));
        }

        // Ensure topic is available for diagnostics on the frontend.
        payload.put("topic", topic);

        Sinks.EmitResult result = sink.tryEmitNext(payload);
        if (result.isFailure()) {
            log.debug("Dropped machine status event due to sink state: {}", result);
        }
    }
}
