package org.sokybot.webview;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.IRSocketFireAndForgetHandler;
import org.sokybot.webview.api.IRSocketStreamHandler;
import org.sokybot.webview.api.IRSocketChannelHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Registry for RSocket handlers using OSGi whiteboard pattern.
 * 
 * Handlers are automatically discovered and registered when they are
 * published as OSGi services with the appropriate service properties.
 */
@Component(service = RSocketHandlerRegistry.class, immediate = true)
public class RSocketHandlerRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(RSocketHandlerRegistry.class);
    
    private final AtomicLong bindOrder = new AtomicLong();
    private final Map<String, RegisteredHandler<IRSocketHandler>> handlers = new ConcurrentHashMap<>();
    private final Map<String, RegisteredHandler<IRSocketStreamHandler>> streamHandlers = new ConcurrentHashMap<>();
    private final Map<String, RegisteredHandler<IRSocketFireAndForgetHandler>> fireAndForgetHandlers = new ConcurrentHashMap<>();
    private final Map<String, RegisteredHandler<IRSocketChannelHandler>> channelHandlers = new ConcurrentHashMap<>();
    private final Map<String, List<String>> requiredParamsByMethod = Map.of(
            "machine.start", List.of("machineId"),
            "machine.stop", List.of("machineId"),
            "group.details", List.of("name"),
            "extension.schema", List.of("pageId"),
            "extension.action", List.of("pageId", "action"),
            "extension.toolbar.action", List.of("actionId", "action"));
    
    // ========== Request-Response Handlers ==========
    
    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    )
    protected void bindHandler(IRSocketHandler handler) {
        bindHandler(handler, null);
    }

    protected void bindHandler(IRSocketHandler handler, Map<String, Object> properties) {
        String[] methods = handler.getMethods();
        if (methods == null || methods.length == 0) {
            logger.warn("Handler {} has no methods defined, skipping", handler.getClass().getName());
            return;
        }
        
        for (String method : methods) {
            if (method != null && !method.isEmpty()) {
                registerMethodHandler(method, handler, properties);
            }
        }
    }
    
    protected void unbindHandler(IRSocketHandler handler) {
        String[] methods = handler.getMethods();
        if (methods != null) {
            for (String method : methods) {
                if (method != null) {
                    unregisterHandler(handlers, method, handler, "RSocket handler");
                }
            }
        }
    }
    
    // ========== Stream Handlers ==========
    
    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    )
    protected void bindStreamHandler(IRSocketStreamHandler handler) {
        bindStreamHandler(handler, null);
    }

    protected void bindStreamHandler(IRSocketStreamHandler handler, Map<String, Object> properties) {
        String stream = handler.getStreamName();
        if (stream != null && !stream.isEmpty()) {
            registerSingleHandler(streamHandlers, stream, handler, resolveRanking(
                    IRSocketStreamHandler.RANKING_PROPERTY,
                    handler.getRanking(), properties), "RSocket stream handler");
        } else {
            logger.warn("Stream handler {} has null/empty stream name, skipping", handler.getClass().getName());
        }
    }
    
    protected void unbindStreamHandler(IRSocketStreamHandler handler) {
        String stream = handler.getStreamName();
        if (stream != null) {
            unregisterHandler(streamHandlers, stream, handler, "RSocket stream handler");
        }
    }
    
    // ========== Handler Lookup and Invocation ==========
    
    /**
     * Handle a request-response RSocket call.
     * 
     * @param request the incoming request
     * @return response Mono
     */
    public Mono<RSocketResponse> handleRequest(RSocketRequest request) {
        RSocketResponse envelopeError = validateEnvelope(request);
        if (envelopeError != null) {
            return Mono.just(envelopeError);
        }
        if (request == null || request.getMethod() == null) {
            return Mono.just(RSocketResponse.error(
                RSocketResponse.ErrorCode.INVALID_REQUEST,
                "Request or method is null"
            ));
        }
        
        String method = request.getMethod();
        RegisteredHandler<IRSocketHandler> registeredHandler = handlers.get(method);
        IRSocketHandler handler = registeredHandler != null ? registeredHandler.handler : null;
        
        if (handler == null) {
            logger.debug("No handler found for method: {}", method);
            return Mono.just(RSocketResponse.methodNotFound(method));
        }
        
        try {
            return handler.handle(request)
                .doOnNext(response -> {
                    if (request.getId() != null && response.getId() == null) {
                        response.setId(request.getId());
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error handling request for method: {}", method, error);
                    return Mono.just(RSocketResponse.internalError(error));
                });
        } catch (Exception e) {
            logger.error("Exception invoking handler for method: {}", method, e);
            return Mono.just(RSocketResponse.internalError(e));
        }
    }
    
    /**
     * Handle a request-stream RSocket call.
     * 
     * @param request the incoming request
     * @return stream Flux
     */
    public Flux<Object> handleStream(RSocketRequest request) {
        RSocketResponse envelopeError = validateEnvelope(request);
        if (envelopeError != null) {
            return Flux.just(envelopeError.toMap());
        }
        if (request == null || request.getMethod() == null) {
            return Flux.just(RSocketResponse.error(
                RSocketResponse.ErrorCode.INVALID_REQUEST,
                "Request or method is null"
            ).toMap());
        }
        
        String streamName = request.getMethod();
        RegisteredHandler<IRSocketStreamHandler> registeredHandler = streamHandlers.get(streamName);
        IRSocketStreamHandler handler = registeredHandler != null ? registeredHandler.handler : null;
        
        if (handler == null) {
            logger.debug("No stream handler found for: {}", streamName);
            return Flux.just(RSocketResponse.error(
                RSocketResponse.ErrorCode.METHOD_NOT_FOUND,
                "Stream not found: " + streamName
            ).toMap());
        }
        
        try {
            return handler.handleStream(request)
                .onErrorResume(error -> {
                    logger.error("Error handling stream: {}", streamName, error);
                    return Flux.just(RSocketResponse.internalError(error).toMap());
                });
        } catch (Exception e) {
            logger.error("Exception invoking stream handler: {}", streamName, e);
            return Flux.just(RSocketResponse.internalError(e).toMap());
        }
    }

    // ========== Fire-and-Forget Handlers ==========
    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    )
    protected void bindFireAndForgetHandler(IRSocketFireAndForgetHandler handler) {
        bindFireAndForgetHandler(handler, null);
    }

    protected void bindFireAndForgetHandler(IRSocketFireAndForgetHandler handler, Map<String, Object> properties) {
        String[] methods = handler.getMethods();
        if (methods == null || methods.length == 0) {
            logger.warn("Fire-and-forget handler {} has no methods defined, skipping", handler.getClass().getName());
            return;
        }
        for (String method : methods) {
            if (method != null && !method.isEmpty()) {
                registerSingleHandler(
                        fireAndForgetHandlers,
                        method,
                        handler,
                        resolveRanking(IRSocketFireAndForgetHandler.RANKING_PROPERTY, handler.getRanking(), properties),
                        "Fire-and-forget handler");
            }
        }
    }

    protected void unbindFireAndForgetHandler(IRSocketFireAndForgetHandler handler) {
        String[] methods = handler.getMethods();
        if (methods == null) {
            return;
        }
        for (String method : methods) {
            if (method != null) {
                unregisterHandler(fireAndForgetHandlers, method, handler, "Fire-and-forget handler");
            }
        }
    }

    public Mono<Void> handleFireAndForget(RSocketRequest request) {
        if (validateEnvelope(request) != null || request == null || request.getMethod() == null) {
            return Mono.empty();
        }
        RegisteredHandler<IRSocketFireAndForgetHandler> registeredHandler = fireAndForgetHandlers.get(request.getMethod());
        IRSocketFireAndForgetHandler handler = registeredHandler != null ? registeredHandler.handler : null;
        if (handler == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("No fire-and-forget handler for method: {} (ignored)", request.getMethod());
            }
            return Mono.empty();
        }
        return handler.handleFireAndForget(request)
            .onErrorResume(error -> {
                logger.error("Error handling fire-and-forget for method: {}", request.getMethod(), error);
                return Mono.empty();
            });
    }

    // ========== Channel Handlers ==========
    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    )
    protected void bindChannelHandler(IRSocketChannelHandler handler) {
        bindChannelHandler(handler, null);
    }

    protected void bindChannelHandler(IRSocketChannelHandler handler, Map<String, Object> properties) {
        String channel = handler.getChannelName();
        if (channel != null && !channel.isEmpty()) {
            registerSingleHandler(
                    channelHandlers,
                    channel,
                    handler,
                    resolveRanking(IRSocketChannelHandler.RANKING_PROPERTY, handler.getRanking(), properties),
                    "Channel handler");
        } else {
            logger.warn("Channel handler {} has null/empty channel name, skipping", handler.getClass().getName());
        }
    }

    protected void unbindChannelHandler(IRSocketChannelHandler handler) {
        String channel = handler.getChannelName();
        if (channel != null) {
            unregisterHandler(channelHandlers, channel, handler, "Channel handler");
        }
    }

    public Flux<Object> handleChannel(RSocketRequest initialRequest, Flux<RSocketRequest> inbound) {
        RSocketResponse envelopeError = validateEnvelope(initialRequest);
        if (envelopeError != null) {
            return Flux.just(envelopeError.toMap());
        }
        if (initialRequest == null || initialRequest.getMethod() == null) {
            return Flux.just(RSocketResponse.error(
                RSocketResponse.ErrorCode.INVALID_REQUEST,
                "Initial channel request or method is null"
            ).toMap());
        }
        RegisteredHandler<IRSocketChannelHandler> registeredHandler = channelHandlers.get(initialRequest.getMethod());
        IRSocketChannelHandler handler = registeredHandler != null ? registeredHandler.handler : null;
        if (handler == null) {
            logger.debug("No channel handler found for: {}", initialRequest.getMethod());
            return Flux.just(RSocketResponse.methodNotFound(initialRequest.getMethod()).toMap());
        }
        return handler.handleChannel(initialRequest, inbound)
            .onBackpressureBuffer(1024, dropped ->
                logger.debug("Dropped channel payload due to overflow: {}", dropped))
            .onErrorResume(error -> {
                logger.error("Error handling channel: {}", initialRequest.getMethod(), error);
                return Flux.just(RSocketResponse.internalError(error).toMap());
            });
    }
    
    /**
     * Check if a handler exists for the given method.
     */
    public boolean hasHandler(String method) {
        return handlers.containsKey(method);
    }
    
    /**
     * Check if a stream handler exists for the given stream name.
     */
    public boolean hasStreamHandler(String streamName) {
        return streamHandlers.containsKey(streamName);
    }
    
    /**
     * Get all registered method names.
     */
    public Collection<String> getRegisteredMethods() {
        return Collections.unmodifiableSet(handlers.keySet());
    }
    
    /**
     * Get all registered stream names.
     */
    public Collection<String> getRegisteredStreams() {
        return Collections.unmodifiableSet(streamHandlers.keySet());
    }
    
    /**
     * Get handler info for debugging/documentation.
     */
    public Map<String, String> getHandlerInfo() {
        Map<String, String> info = new ConcurrentHashMap<>();
        handlers.forEach((method, handler) ->
            info.put(method, handler.handler.getDescription())
        );
        return info;
    }
    
    /**
     * Get stream handler info for debugging/documentation.
     */
    public Map<String, String> getStreamHandlerInfo() {
        Map<String, String> info = new ConcurrentHashMap<>();
        streamHandlers.forEach((stream, handler) ->
            info.put(stream, handler.handler.getDescription())
        );
        return info;
    }

    private void registerMethodHandler(String method, IRSocketHandler handler, Map<String, Object> properties) {
        registerSingleHandler(
                handlers,
                method,
                handler,
                resolveRanking(IRSocketHandler.RANKING_PROPERTY, handler.getRanking(), properties),
                "RSocket handler");
    }

    private int resolveRanking(String rankingProperty, int fallbackRanking, Map<String, Object> properties) {
        if (properties != null) {
            Object raw = properties.get(rankingProperty);
            if (raw instanceof Number) {
                return ((Number) raw).intValue();
            }
            if (raw instanceof String) {
                try {
                    return Integer.parseInt(((String) raw).trim());
                } catch (NumberFormatException e) {
                    logger.warn("Invalid ranking value '{}' for property '{}', using fallback {}", raw, rankingProperty,
                            fallbackRanking);
                }
            }
        }
        return fallbackRanking;
    }

    private <T> void registerSingleHandler(
            Map<String, RegisteredHandler<T>> registry,
            String key,
            T handler,
            int ranking,
            String kind) {
        RegisteredHandler<T> candidate = new RegisteredHandler<>(handler, ranking, bindOrder.incrementAndGet());
        registry.compute(key, (method, existing) -> {
            if (existing == null) {
                logger.info("Registered {}: {} -> {} (ranking={})", kind, method, className(handler), ranking);
                return candidate;
            }
            if (existing.ranking > candidate.ranking) {
                logger.debug(
                        "Ignored {} for {} because existing handler has higher ranking ({} > {})",
                        kind, method, existing.ranking, candidate.ranking);
                return existing;
            }
            if (existing.ranking < candidate.ranking) {
                logger.info(
                        "Replaced {} for {} with higher-ranked handler {} ({} -> {})",
                        kind, method, className(handler), existing.ranking, candidate.ranking);
                return candidate;
            }
            logger.error(
                    "Equal-ranking collision for {} '{}': keeping older {} and ignoring {} (ranking={})",
                    kind, method, className(existing.handler), className(handler), candidate.ranking);
            return existing;
        });
    }

    private <T> void unregisterHandler(Map<String, RegisteredHandler<T>> registry, String key, T handler, String kind) {
        registry.computeIfPresent(key, (method, existing) -> {
            if (existing.handler == handler) {
                logger.info("Unregistered {}: {}", kind, method);
                return null;
            }
            return existing;
        });
    }

    private static String className(Object handler) {
        return handler.getClass().getSimpleName();
    }

    private static final class RegisteredHandler<T> {
        private final T handler;
        private final int ranking;
        @SuppressWarnings("unused")
        private final long bindOrder;

        private RegisteredHandler(T handler, int ranking, long bindOrder) {
            this.handler = handler;
            this.ranking = ranking;
            this.bindOrder = bindOrder;
        }
    }

    private RSocketResponse validateEnvelope(RSocketRequest request) {
        if (request == null) {
            return RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_REQUEST,
                    "Request or method is null");
        }
        String method = request.getMethod();
        if (method == null || method.trim().isEmpty()) {
            return RSocketResponse.invalidParams("method is required");
        }
        List<String> required = requiredParamsByMethod.get(method);
        if (required == null || required.isEmpty()) {
            return null;
        }
        Map<String, Object> params = request.getParams();
        if (params == null) {
            return RSocketResponse.invalidParams("params are required for " + method);
        }
        for (String key : required) {
            Object value = params.get(key);
            if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
                return RSocketResponse.invalidParams("Missing required param: " + key);
            }
        }
        return null;
    }
}
