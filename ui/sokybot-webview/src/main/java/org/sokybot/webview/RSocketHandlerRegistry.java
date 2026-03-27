package org.sokybot.webview;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    
    private final Map<String, IRSocketHandler> handlers = new ConcurrentHashMap<>();
    private final Map<String, IRSocketStreamHandler> streamHandlers = new ConcurrentHashMap<>();
    private final Map<String, IRSocketFireAndForgetHandler> fireAndForgetHandlers = new ConcurrentHashMap<>();
    private final Map<String, IRSocketChannelHandler> channelHandlers = new ConcurrentHashMap<>();
    
    // ========== Request-Response Handlers ==========
    
    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC
    )
    protected void bindHandler(IRSocketHandler handler) {
        String[] methods = handler.getMethods();
        if (methods == null || methods.length == 0) {
            logger.warn("Handler {} has no methods defined, skipping", handler.getClass().getName());
            return;
        }
        
        for (String method : methods) {
            if (method != null && !method.isEmpty()) {
                handlers.put(method, handler);
                logger.info("Registered RSocket handler: {} -> {}", method, handler.getClass().getSimpleName());
            }
        }
    }
    
    protected void unbindHandler(IRSocketHandler handler) {
        String[] methods = handler.getMethods();
        if (methods != null) {
            for (String method : methods) {
                if (method != null) {
                    handlers.remove(method);
                    logger.info("Unregistered RSocket handler: {}", method);
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
        String stream = handler.getStreamName();
        if (stream != null && !stream.isEmpty()) {
            streamHandlers.put(stream, handler);
            logger.info("Registered RSocket stream handler: {} -> {}", stream, handler.getClass().getSimpleName());
        } else {
            logger.warn("Stream handler {} has null/empty stream name, skipping", handler.getClass().getName());
        }
    }
    
    protected void unbindStreamHandler(IRSocketStreamHandler handler) {
        String stream = handler.getStreamName();
        if (stream != null) {
            streamHandlers.remove(stream);
            logger.info("Unregistered RSocket stream handler: {}", stream);
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
        if (request == null || request.getMethod() == null) {
            return Mono.just(RSocketResponse.error(
                RSocketResponse.ErrorCode.INVALID_REQUEST,
                "Request or method is null"
            ));
        }
        
        String method = request.getMethod();
        IRSocketHandler handler = handlers.get(method);
        
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
        if (request == null || request.getMethod() == null) {
            return Flux.just(RSocketResponse.error(
                RSocketResponse.ErrorCode.INVALID_REQUEST,
                "Request or method is null"
            ).toMap());
        }
        
        String streamName = request.getMethod();
        IRSocketStreamHandler handler = streamHandlers.get(streamName);
        
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
        String[] methods = handler.getMethods();
        if (methods == null || methods.length == 0) {
            logger.warn("Fire-and-forget handler {} has no methods defined, skipping", handler.getClass().getName());
            return;
        }
        for (String method : methods) {
            if (method != null && !method.isEmpty()) {
                fireAndForgetHandlers.put(method, handler);
                logger.info("Registered fire-and-forget handler: {} -> {}", method, handler.getClass().getSimpleName());
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
                fireAndForgetHandlers.remove(method);
                logger.info("Unregistered fire-and-forget handler: {}", method);
            }
        }
    }

    public Mono<Void> handleFireAndForget(RSocketRequest request) {
        if (request == null || request.getMethod() == null) {
            return Mono.empty();
        }
        IRSocketFireAndForgetHandler handler = fireAndForgetHandlers.get(request.getMethod());
        if (handler == null) {
            logger.debug("No fire-and-forget handler found for method: {}", request.getMethod());
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
        String channel = handler.getChannelName();
        if (channel != null && !channel.isEmpty()) {
            channelHandlers.put(channel, handler);
            logger.info("Registered channel handler: {} -> {}", channel, handler.getClass().getSimpleName());
        } else {
            logger.warn("Channel handler {} has null/empty channel name, skipping", handler.getClass().getName());
        }
    }

    protected void unbindChannelHandler(IRSocketChannelHandler handler) {
        String channel = handler.getChannelName();
        if (channel != null) {
            channelHandlers.remove(channel);
            logger.info("Unregistered channel handler: {}", channel);
        }
    }

    public Flux<Object> handleChannel(RSocketRequest initialRequest, Flux<RSocketRequest> inbound) {
        if (initialRequest == null || initialRequest.getMethod() == null) {
            return Flux.just(RSocketResponse.error(
                RSocketResponse.ErrorCode.INVALID_REQUEST,
                "Initial channel request or method is null"
            ).toMap());
        }
        IRSocketChannelHandler handler = channelHandlers.get(initialRequest.getMethod());
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
            info.put(method, handler.getDescription())
        );
        return info;
    }
    
    /**
     * Get stream handler info for debugging/documentation.
     */
    public Map<String, String> getStreamHandlerInfo() {
        Map<String, String> info = new ConcurrentHashMap<>();
        streamHandlers.forEach((stream, handler) -> 
            info.put(stream, handler.getDescription())
        );
        return info;
    }
}
