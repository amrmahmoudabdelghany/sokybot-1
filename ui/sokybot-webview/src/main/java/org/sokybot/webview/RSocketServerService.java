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
import org.reactivestreams.Publisher;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.sokybot.http.server.api.IWebSocketRegistry;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.rsocket.exceptions.RejectedSetupException;

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
        extensionEventSink.emitNext(event, (signalType, emitResult) -> {
            if (emitResult.isSuccess()) {
                return true;
            }
            logger.debug("Failed to emit extension event {} due to {}", signalType, emitResult);
            return emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED;
        });
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

                try {
                    validateClientSetup(setup);
                } catch (RejectedSetupException e) {
                    logger.warn("RSocket SETUP rejected: {}", e.getMessage());
                    return Mono.error(e);
                }

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

                    @Override
                    public Mono<Void> fireAndForget(Payload payload) {
                        String requestData = payload.getDataUtf8();
                        logger.debug("Received fire-and-forget request: {}", requestData);
                        return handleFireAndForget(requestData);
                    }

                    @Override
                    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
                        logger.debug("Received request-channel request");
                        return handleRequestChannel(payloads);
                    }
                });
            }
        };
    }

    /**
     * Parse optional JSON SETUP payload from the client. Rejects the connection if the client
     * requires a newer {@link WebviewProtocolConstants#PROTOCOL_API_VERSION} than this server.
     */
    private void validateClientSetup(ConnectionSetupPayload setup) {
        if (!setup.data().isReadable()) {
            return;
        }
        String raw = setup.getDataUtf8();
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = mapper.readValue(raw, Map.class);
            Object min = m.get(WebviewProtocolConstants.SETUP_KEY_MIN_PROTOCOL_API);
            if (min instanceof Number) {
                int required = ((Number) min).intValue();
                if (required > WebviewProtocolConstants.PROTOCOL_API_VERSION) {
                    throw new RejectedSetupException("Client requires minProtocolApi=" + required
                            + " but server supports up to " + WebviewProtocolConstants.PROTOCOL_API_VERSION);
                }
            }
            Object uiId = m.get(WebviewProtocolConstants.SETUP_KEY_UI_BUILD_ID);
            if (uiId != null) {
                logger.debug("RSocket client uiBuildId={}", uiId);
            }
        } catch (RejectedSetupException e) {
            throw e;
        } catch (Exception e) {
            logger.debug("Ignoring non-JSON or malformed SETUP data: {}", e.toString());
        }
    }

    private Mono<Void> handleFireAndForget(String requestData) {
        try {
            RSocketRequest request = parseRequest(requestData);
            if (request == null || request.getMethod() == null) {
                return Mono.empty();
            }
            return handlerRegistry.handleFireAndForget(request);
        } catch (Exception e) {
            logger.error("Error processing fire-and-forget request", e);
            return Mono.empty();
        }
    }

    private Flux<Payload> handleRequestChannel(Publisher<Payload> payloads) {
        Flux<RSocketRequest> requests = Flux.from(payloads)
                .map(payload -> parseRequest(payload.getDataUtf8()))
                .filter(request -> request != null && request.getMethod() != null);

        return requests.switchOnFirst((signal, flux) -> {
            if (!signal.hasValue()) {
                return Flux.just(toPayload(RSocketResponse.error(
                        RSocketResponse.ErrorCode.INVALID_REQUEST,
                        "Channel requires an initial request payload")));
            }
            RSocketRequest initial = signal.get();
            return handlerRegistry.handleChannel(initial, flux.skip(1))
                    .map(data -> {
                        try {
                            return DefaultPayload.create(mapper.writeValueAsString(data));
                        } catch (JsonProcessingException e) {
                            logger.error("Error serializing channel data", e);
                            return DefaultPayload.create("{\"error\":\"serialization_failed\"}");
                        }
                    })
                    .onBackpressureBuffer(1024, dropped -> logger.debug("Dropped channel frame due to overflow: {}", dropped))
                    .onErrorResume(error -> {
                        logger.error("Error handling request-channel", error);
                        return Flux.just(toPayload(RSocketResponse.internalError(error)));
                    });
        });
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
     * Parse request data from structured JSON envelope only.
     */
    @SuppressWarnings("unchecked")
    private RSocketRequest parseRequest(String requestData) {
        if (requestData == null || requestData.isEmpty()) {
            return null;
        }
        if (!requestData.startsWith("{")) {
            return null;
        }
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
            logger.debug("Failed to parse request JSON: {}", e.getMessage());
            return null;
        }
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
