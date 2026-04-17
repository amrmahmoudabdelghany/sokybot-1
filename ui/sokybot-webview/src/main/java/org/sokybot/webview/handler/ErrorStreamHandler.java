package org.sokybot.webview.handler;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.commons.error.ErrorInfo;
import org.sokybot.commons.error.IErrorRegistry;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.IRSocketStreamHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * RSocket handler for error operations.
 * 
 * Methods:
 * - error.list: Get recent errors
 * - error.clear: Clear error history
 * - error.count: Get error count
 * 
 * Streams:
 * - error.stream: Subscribe to real-time error stream
 */
@Component(service = { IRSocketHandler.class, IRSocketStreamHandler.class }, property = {
        IRSocketHandler.METHOD_PROPERTY + "=error.list",
        IRSocketHandler.METHOD_PROPERTY + "=error.clear",
        IRSocketHandler.METHOD_PROPERTY + "=error.count",
        IRSocketStreamHandler.STREAM_PROPERTY + "=error.stream"
})
public class ErrorStreamHandler implements IRSocketHandler, IRSocketStreamHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private volatile IErrorRegistry errorRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setErrorRegistry(IErrorRegistry errorRegistry) {
        this.errorRegistry = errorRegistry;
    }

    protected void unsetErrorRegistry(IErrorRegistry errorRegistry) {
        this.errorRegistry = null;
    }

    @Override
    public String[] getMethods() {
        return new String[] { "error.list", "error.clear", "error.count" };
    }

    @Override
    public String getStreamName() {
        return "error.stream";
    }

    @Override
    public String getDescription() {
        return "Error registry operations (list, clear, stream)";
    }

    @Override
    public int getRanking() {
        return 0;
    }

    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        String method = request.getMethod();

        switch (method) {
            case "error.list":
                return handleList(request);
            case "error.clear":
                return handleClear(request);
            case "error.count":
                return handleCount(request);
            default:
                return Mono.just(RSocketResponse.methodNotFound(method));
        }
    }

    @Override
    public Flux<Object> handleStream(RSocketRequest request) {
        if (errorRegistry == null) {
            return Flux.error(new IllegalStateException("Error registry not available"));
        }

        return errorRegistry.getErrorStream()
                .map(this::errorToMap);
    }

    private Mono<RSocketResponse> handleList(RSocketRequest request) {
        if (errorRegistry == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Error registry not available"));
        }

        Integer limit = request.getInt("limit");
        int effectiveLimit = (limit != null && limit > 0) ? limit : 50;

        List<ErrorInfo> errors = errorRegistry.getRecentErrors(effectiveLimit);
        List<Map<String, Object>> result = errors.stream()
                .map(this::errorToMap)
                .collect(Collectors.toList());

        return Mono.just(RSocketResponse.success(result));
    }

    private Mono<RSocketResponse> handleClear(RSocketRequest request) {
        if (errorRegistry == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Error registry not available"));
        }

        errorRegistry.clearErrors();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "cleared");
        return Mono.just(RSocketResponse.success(result));
    }

    private Mono<RSocketResponse> handleCount(RSocketRequest request) {
        if (errorRegistry == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Error registry not available"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("count", errorRegistry.getErrorCount());
        return Mono.just(RSocketResponse.success(result));
    }

    private Map<String, Object> errorToMap(ErrorInfo error) {
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", TIMESTAMP_FORMATTER.format(error.getTimestamp()));
        map.put("category", error.getCategory().name());
        map.put("source", error.getSource());
        map.put("message", error.getMessage());

        if (error.getMachineId() != null) {
            map.put("machineId", error.getMachineId());
        }
        if (error.getStackTrace() != null) {
            map.put("stackTrace", error.getStackTrace());
        }

        return map;
    }
}
