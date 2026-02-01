package org.sokybot.webview.handler;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.store.IEventStore;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;
import org.sokybot.webview.api.doc.RSocketMethod;
import org.sokybot.webview.api.doc.RSocketParam;

import reactor.core.publisher.Mono;

/**
 * RSocket handler for accessing the event store.
 */
@Component(service = IRSocketHandler.class, property = {
        IRSocketHandler.METHOD_PROPERTY + "=eventstore.recent",
        IRSocketHandler.METHOD_PROPERTY + "=eventstore.query",
        IRSocketHandler.METHOD_PROPERTY + "=eventstore.clear"
})
@RSocketMethod(name = "eventstore.recent", description = "Get recent events for a machine", params = {
        @RSocketParam(name = "machineId", description = "Machine ID"),
        @RSocketParam(name = "limit", type = "integer", required = false, description = "Max number of events (default 50)")
})
@RSocketMethod(name = "eventstore.query", description = "Query events by time range", params = {
        @RSocketParam(name = "machineId", description = "Machine ID"),
        @RSocketParam(name = "startTime", description = "ISO-8601 Start Time"),
        @RSocketParam(name = "endTime", description = "ISO-8601 End Time")
})
@RSocketMethod(name = "eventstore.clear", description = "Clear event history for a machine", params = {
        @RSocketParam(name = "machineId", description = "Machine ID")
})
public class EventStoreHandler implements IRSocketHandler {

    private volatile IEventStore eventStore;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setEventStore(IEventStore store) {
        this.eventStore = store;
    }

    protected void unsetEventStore(IEventStore store) {
        this.eventStore = null;
    }

    @Override
    public String[] getMethods() {
        return new String[] { "eventstore.recent", "eventstore.query", "eventstore.clear" };
    }

    @Override
    public String getDescription() {
        return "Game event history access";
    }

    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        if (eventStore == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Event store not available"));
        }

        String method = request.getMethod();

        switch (method) {
            case "eventstore.recent":
                return handleRecent(request);
            case "eventstore.query":
                return handleQuery(request);
            case "eventstore.clear":
                return handleClear(request);
            default:
                return Mono.just(RSocketResponse.methodNotFound(method));
        }
    }

    private Mono<RSocketResponse> handleRecent(RSocketRequest request) {
        String machineId = request.getString("machineId");
        if (machineId == null || machineId.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: machineId"));
        }

        int limit = request.getInt("limit", 50);
        List<IGameEvent> events = eventStore.getRecentEvents(machineId, limit);

        Map<String, Object> response = new HashMap<>();
        response.put("machineId", machineId);
        response.put("events", events);
        response.put("count", events.size());

        return Mono.just(RSocketResponse.success(response));
    }

    private Mono<RSocketResponse> handleQuery(RSocketRequest request) {
        String machineId = request.getString("machineId");
        if (machineId == null || machineId.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: machineId"));
        }

        String startStr = request.getString("startTime");
        String endStr = request.getString("endTime");

        if (startStr == null || endStr == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameters: startTime, endTime"));
        }

        try {
            Instant start = Instant.parse(startStr);
            Instant end = Instant.parse(endStr);

            List<IGameEvent> events = eventStore.getEvents(machineId, start, end);

            Map<String, Object> response = new HashMap<>();
            response.put("machineId", machineId);
            response.put("events", events);
            response.put("count", events.size());

            return Mono.just(RSocketResponse.success(response));
        } catch (Exception e) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Invalid time format (use ISO-8601): " + e.getMessage()));
        }
    }

    private Mono<RSocketResponse> handleClear(RSocketRequest request) {
        String machineId = request.getString("machineId");
        if (machineId == null || machineId.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: machineId"));
        }

        eventStore.clear(machineId);

        Map<String, Object> response = new HashMap<>();
        response.put("machineId", machineId);
        response.put("cleared", true);

        return Mono.just(RSocketResponse.success(response));
    }
}
