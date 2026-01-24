package org.sokybot.webview;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.sokybot.http.server.api.IWebSocketRegistry;
import org.sokybot.webview.api.IWebviewConfigurator;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * RSocket server service that registers with the shared HTTP server's WebSocket
 * registry.
 * Handles RSocket communication for the webview frontend at the /rsocket path.
 * 
 * <p>
 * This service uses a structured JSON protocol for request/response:
 * 
 * <pre>
 * Request:
 * { "method": "character.state", "params": { "machineId": "..." }, "id": "req-1" }
 * 
 * Response:
 * { "result": { ... }, "id": "req-1" }
 * or
 * { "error": { "code": -32601, "message": "Method not found" }, "id": "req-1" }
 * </pre>
 * 
 * <p>
 * Handlers are discovered via OSGi whiteboard pattern through
 * {@link RSocketHandlerRegistry}.
 */
@Component(service = RSocketServerService.class, immediate = true)
public class RSocketServerService {

    private static final Logger logger = LoggerFactory.getLogger(RSocketServerService.class);
    private static final String RSOCKET_PATH = "/rsocket";

    private final ObjectMapper mapper = new ObjectMapper();

    // RSocket connection acceptor
    private io.rsocket.transport.ServerTransport.ConnectionAcceptor connectionAcceptor;

    // Dependencies
    private IWebSocketRegistry wsRegistry;
    private RSocketHandlerRegistry handlerRegistry;

    private volatile IWebviewConfigurator extensionConfigurator;

    // Event sink for extension events
    private final Sinks.Many<Map<String, Object>> extensionEventSink = Sinks.many().multicast()
            .onBackpressureBuffer(100);

    // Stream handlers for dynamic extension streams (registered by
    // WebviewConfigurator)
    private final Map<String, Function<Map<String, Object>, Flux<Map<String, Object>>>> dynamicStreamHandlers = new java.util.concurrent.ConcurrentHashMap<>();

    @Reference
    protected void setWsRegistry(IWebSocketRegistry wsRegistry) {
        this.wsRegistry = wsRegistry;
    }

    @Reference
    protected void setHandlerRegistry(RSocketHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setExtensionConfigurator(IWebviewConfigurator configurator) {
        this.extensionConfigurator = configurator;
    }

    protected void unsetExtensionConfigurator(IWebviewConfigurator configurator) {
        this.extensionConfigurator = null;
    }

    /**
     * Register extension configurator (called by WebviewConfigurator).
     * 
     * @deprecated Use OSGi @Reference instead
     */
    @Deprecated
    public void registerExtensionConfigurator(WebviewConfigurator configurator) {
        // No longer needed - using @Reference
    }

    /**
     * Register a dynamic stream handler for extension streams.
     */
    public void registerStreamHandler(String streamKey,
            Function<Map<String, Object>, Flux<Map<String, Object>>> handler) {
        dynamicStreamHandlers.put(streamKey, handler);
        logger.debug("Registered dynamic stream handler: {}", streamKey);
    }

    /**
     * Unregister a dynamic stream handler.
     */
    public void unregisterStreamHandler(String streamKey) {
        dynamicStreamHandlers.remove(streamKey);
        logger.debug("Unregistered dynamic stream handler: {}", streamKey);
    }

    /**
     * Send an extension event to all connected clients.
     */
    public void sendExtensionEvent(String eventType, Map<String, Object> data) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", eventType);
        event.put("data", data);
        event.put("timestamp", System.currentTimeMillis());
        extensionEventSink.tryEmitNext(event);
    }

    /**
     * Get the extension event stream for streaming to clients.
     */
    public Flux<Map<String, Object>> getExtensionEventStream() {
        return extensionEventSink.asFlux();
    }

    @Activate
    public void start() {
        logger.info("Starting RSocket service, registering WebSocket handler at {}", RSOCKET_PATH);

        // Create the RSocket connection acceptor from the socket acceptor
        SocketAcceptor socketAcceptor = createAcceptor();
        this.connectionAcceptor = RSocketServer.create(socketAcceptor).asConnectionAcceptor();

        // Register WebSocket handler with the shared HTTP server
        wsRegistry.registerHandler(RSOCKET_PATH, (in, out) -> {
            logger.debug("New RSocket WebSocket connection from {}",
                    ((reactor.netty.Connection) in).channel().remoteAddress());

            io.rsocket.transport.netty.WebsocketDuplexConnection connection = new io.rsocket.transport.netty.WebsocketDuplexConnection(
                    (reactor.netty.Connection) in);

            return connectionAcceptor.apply(connection)
                    .doOnError(error -> logger.error("RSocket connection error", error))
                    .then(connection.onClose());
        });

        logger.info("RSocket service registered at {} on port {}", RSOCKET_PATH, wsRegistry.getPort());
    }

    private SocketAcceptor createAcceptor() {
        return new SocketAcceptor() {
            @Override
            public Mono<RSocket> accept(ConnectionSetupPayload setup, RSocket sendingSocket) {
                logger.info("RSocket connection accepted - Data MIME: {}, Metadata MIME: {}",
                        setup.dataMimeType(), setup.metadataMimeType());

                return Mono.just(new RSocket() {
                    @Override
                    public Mono<Payload> requestResponse(Payload payload) {
                        String requestData = payload.getDataUtf8();
                        logger.debug("Received request: {}", requestData);
                        return handleRequestResponse(requestData);
                    }

                    @Override
                    public Flux<Payload> requestStream(Payload payload) {
                        String requestData = payload.getDataUtf8();
                        logger.debug("Received stream request: {}", requestData);
                        return handleRequestStream(requestData);
                    }
                });
            }
        };
    }

    private Mono<Payload> handleRequestResponse(String requestData) {
        try {
            // Parse the request
            RSocketRequest request = parseRequest(requestData);

            if (request == null || request.getMethod() == null) {
                return errorPayload(RSocketResponse.error(
                        RSocketResponse.ErrorCode.PARSE_ERROR,
                        "Failed to parse request"));
            }

            // Delegate to handler registry
            return handlerRegistry.handleRequest(request)
                    .map(this::toPayload)
                    .onErrorResume(error -> {
                        logger.error("Error handling request", error);
                        return errorPayload(RSocketResponse.internalError(error));
                    });

        } catch (Exception e) {
            logger.error("Error processing request", e);
            return errorPayload(RSocketResponse.internalError(e));
        }
    }

    private Flux<Payload> handleRequestStream(String requestData) {
        try {
            RSocketRequest request = parseRequest(requestData);

            if (request == null || request.getMethod() == null) {
                return Flux.just(toPayload(RSocketResponse.error(
                        RSocketResponse.ErrorCode.PARSE_ERROR,
                        "Failed to parse request")));
            }

            String method = request.getMethod();

            // Check for dynamic extension streams first
            if (method.startsWith("extension.stream:")) {
                return handleDynamicExtensionStream(request);
            }

            // Delegate to handler registry
            return handlerRegistry.handleStream(request)
                    .map(data -> {
                        try {
                            return DefaultPayload.create(mapper.writeValueAsString(data));
                        } catch (JsonProcessingException e) {
                            logger.error("Error serializing stream data", e);
                            return DefaultPayload.create("{\"error\":\"serialization_failed\"}");
                        }
                    })
                    .onErrorResume(error -> {
                        logger.error("Error handling stream", error);
                        return Flux.just(toPayload(RSocketResponse.internalError(error)));
                    });

        } catch (Exception e) {
            logger.error("Error processing stream request", e);
            return Flux.just(toPayload(RSocketResponse.internalError(e)));
        }
    }

    /**
     * Handle dynamic extension streams registered by WebviewConfigurator.
     */
    private Flux<Payload> handleDynamicExtensionStream(RSocketRequest request) {
        // Parse stream key from method: "extension.stream:pageId:streamId"
        String method = request.getMethod();
        String[] parts = method.split(":", 3);

        if (parts.length < 3) {
            return Flux.just(toPayload(RSocketResponse.invalidParams(
                    "Invalid stream format. Expected: extension.stream:pageId:streamId")));
        }

        String pageId = parts[1];
        String streamId = parts[2];
        String streamKey = pageId + ":" + streamId;

        Function<Map<String, Object>, Flux<Map<String, Object>>> handler = dynamicStreamHandlers.get(streamKey);

        if (handler == null) {
            logger.warn("No dynamic stream handler found for: {}", streamKey);
            return Flux.just(toPayload(RSocketResponse.notFound("Stream not found: " + streamKey)));
        }

        return handler.apply(request.getParams())
                .map(data -> {
                    try {
                        return DefaultPayload.create(mapper.writeValueAsString(data));
                    } catch (JsonProcessingException e) {
                        logger.error("Error serializing stream data", e);
                        return DefaultPayload.create("{\"error\":\"serialization_failed\"}");
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error in dynamic stream: {}", streamKey, error);
                    return Flux.just(toPayload(RSocketResponse.internalError(error)));
                });
    }

    /**
     * Parse request data. Supports both new JSON format and legacy string format
     * for backwards compatibility.
     */
    @SuppressWarnings("unchecked")
    private RSocketRequest parseRequest(String requestData) {
        if (requestData == null || requestData.isEmpty()) {
            return null;
        }

        // Try to parse as JSON first (new format)
        if (requestData.startsWith("{")) {
            try {
                Map<String, Object> json = mapper.readValue(requestData, Map.class);
                RSocketRequest request = new RSocketRequest();
                request.setMethod((String) json.get("method"));
                request.setId((String) json.get("id"));

                Object params = json.get("params");
                if (params instanceof Map) {
                    request.setParams((Map<String, Object>) params);
                }

                return request;
            } catch (Exception e) {
                logger.debug("Failed to parse as JSON, trying legacy format: {}", e.getMessage());
            }
        }

        // Legacy format support: "method:param" or "method"
        return parseLegacyRequest(requestData);
    }

    /**
     * Parse legacy string-based request format for backwards compatibility.
     */
    private RSocketRequest parseLegacyRequest(String requestData) {
        RSocketRequest request = new RSocketRequest();
        Map<String, Object> params = new HashMap<>();

        // Map legacy methods to new methods
        if (requestData.startsWith("getCharacterState")) {
            request.setMethod("character.state");
            if (requestData.contains(":")) {
                params.put("machineId", requestData.substring(requestData.indexOf(":") + 1));
            }
        } else if (requestData.startsWith("startBot:")) {
            request.setMethod("machine.start");
            params.put("machineId", requestData.substring(9));
        } else if (requestData.startsWith("stopBot:")) {
            request.setMethod("machine.stop");
            params.put("machineId", requestData.substring(8));
        } else if (requestData.equals("getMachines")) {
            request.setMethod("machine.list");
        } else if (requestData.equals("getGroups")) {
            request.setMethod("group.list");
        } else if (requestData.startsWith("getGroupDetails:")) {
            request.setMethod("group.details");
            params.put("name", requestData.substring(16));
        } else if (requestData.startsWith("createGroup:")) {
            request.setMethod("group.create");
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = mapper.readValue(requestData.substring(12), Map.class);
                params.putAll(parsed);
            } catch (Exception e) {
                logger.warn("Failed to parse createGroup params", e);
            }
        } else if (requestData.startsWith("createMachine:")) {
            request.setMethod("machine.create");
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = mapper.readValue(requestData.substring(14), Map.class);
                params.putAll(parsed);
            } catch (Exception e) {
                logger.warn("Failed to parse createMachine params", e);
            }
        } else if (requestData.startsWith("fs.list")) {
            request.setMethod("fs.list");
            if (requestData.contains(":")) {
                params.put("path", requestData.substring(requestData.indexOf(":") + 1));
            }
        } else if (requestData.equals("fs.roots")) {
            request.setMethod("fs.roots");
        } else if (requestData.startsWith("extension.schema:")) {
            request.setMethod("extension.schema");
            String[] parts = requestData.split(":", 3);
            if (parts.length > 1)
                params.put("pageId", parts[1]);
            if (parts.length > 2)
                params.put("machineId", parts[2]);
        } else if (requestData.startsWith("extension.action:")) {
            request.setMethod("extension.action");
            String[] parts = requestData.split(":", 4);
            if (parts.length > 1)
                params.put("pageId", parts[1]);
            if (parts.length > 2)
                params.put("action", parts[2]);
            if (parts.length > 3) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = mapper.readValue(parts[3], Map.class);
                    params.put("data", data);
                } catch (Exception e) {
                    logger.debug("Failed to parse action data", e);
                }
            }
        } else if (requestData.equals("extension.registry")) {
            request.setMethod("extension.registry");
        } else if (requestData.startsWith("extension.toolbar.action:")) {
            request.setMethod("extension.toolbar.action");
            String[] parts = requestData.split(":", 4);
            if (parts.length > 1)
                params.put("actionId", parts[1]);
            if (parts.length > 2)
                params.put("action", parts[2]);
            if (parts.length > 3) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = mapper.readValue(parts[3], Map.class);
                    params.put("data", data);
                } catch (Exception e) {
                    logger.debug("Failed to parse toolbar action data", e);
                }
            }
        } else if (requestData.equals("stream.events") || requestData.equals("extension.events")) {
            // Legacy stream requests
            request.setMethod(requestData.equals("stream.events") ? "game.events" : "extension.events");
        } else {
            // Unknown legacy method - pass through
            request.setMethod(requestData);
        }

        request.setParams(params);
        return request;
    }

    private Payload toPayload(RSocketResponse response) {
        try {
            return DefaultPayload.create(mapper.writeValueAsString(response.toMap()));
        } catch (JsonProcessingException e) {
            logger.error("Error serializing response", e);
            return DefaultPayload.create("{\"error\":{\"code\":-32603,\"message\":\"Serialization error\"}}");
        }
    }

    private Mono<Payload> errorPayload(RSocketResponse response) {
        return Mono.just(toPayload(response));
    }

    @Deactivate
    public void stop() {
        logger.info("Stopping RSocket service");

        // Unregister from WebSocket registry
        if (wsRegistry != null) {
            wsRegistry.unregisterHandler(RSOCKET_PATH);
            logger.info("RSocket WebSocket handler unregistered from {}", RSOCKET_PATH);
        }
    }
}
