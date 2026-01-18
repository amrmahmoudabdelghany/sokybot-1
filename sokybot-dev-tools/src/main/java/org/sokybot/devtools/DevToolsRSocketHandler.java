package org.sokybot.devtools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.rsocket.core.RSocketServer;
import io.rsocket.RSocket;
import io.rsocket.Payload;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * RSocket handler for dev-tools requests.
 * Handles request-response and stream requests prefixed with "devtools:".
 */
public class DevToolsRSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(DevToolsRSocketHandler.class);
    
    private final ObjectMapper mapper = new ObjectMapper();
    private final BundleManagementService bundleService;
    private final ServiceInspectionService serviceService;
    private final RuntimeMetricsService metricsService;
    private final LogStreamingService logService;
    
    private CloseableChannel server;
    private final Sinks.Many<Map<String, Object>> bundleEventSink;
    private final Sinks.Many<Map<String, Object>> metricsEventSink;
    
    public DevToolsRSocketHandler(BundleManagementService bundleService,
                                  ServiceInspectionService serviceService,
                                  RuntimeMetricsService metricsService,
                                  LogStreamingService logService) {
        this.bundleService = bundleService;
        this.serviceService = serviceService;
        this.metricsService = metricsService;
        this.logService = logService;
        
        // Create event sinks for streaming
        this.bundleEventSink = Sinks.many().multicast().onBackpressureBuffer(100);
        this.metricsEventSink = Sinks.many().multicast().onBackpressureBuffer(100);
    }
    
    /**
     * Start the RSocket server.
     */
    public void start(int port) {
        logger.info("Starting DevTools RSocket Server on port {}", port);
        
        try {
            this.server = RSocketServer.create(new SocketAcceptor() {
                @Override
                public Mono<RSocket> accept(ConnectionSetupPayload setup, RSocket sendingSocket) {
                    return Mono.just(new RSocket() {
                        @Override
                        public Mono<Payload> requestResponse(Payload payload) {
                            return handleRequestResponse(payload);
                        }
                        
                        @Override
                        public Flux<Payload> requestStream(Payload payload) {
                            return handleRequestStream(payload);
                        }
                    });
                }
            })
            .bind(WebsocketServerTransport.create("localhost", port))
            .block();
            
            if (this.server != null) {
                logger.info("DevTools RSocket Server started on port {}", port);
                // Start metrics stream
                startMetricsStream();
            } else {
                throw new RuntimeException("Failed to start RSocket server - server is null");
            }
        } catch (Exception e) {
            logger.error("Failed to start RSocket server on port {}", port, e);
            throw new RuntimeException("Failed to start RSocket server on port " + port, e);
        }
    }
    
    /**
     * Stop the RSocket server.
     */
    public void stop() {
        if (server != null) {
            server.dispose();
            logger.info("DevTools RSocket Server stopped");
        }
    }
    
    /**
     * Handle request-response requests.
     */
    private Mono<Payload> handleRequestResponse(Payload payload) {
        String requestData = payload.getDataUtf8();
        logger.debug("Received devtools request: {}", requestData);
        
        try {
            Map<String, Object> response = new HashMap<>();
            
            if (requestData.startsWith("devtools:bundles.list")) {
                response.put("success", true);
                response.put("data", bundleService.listBundles());
                
            } else if (requestData.startsWith("devtools:bundles.get:")) {
                String symbolicName = requestData.substring("devtools:bundles.get:".length());
                Map<String, Object> bundle = bundleService.getBundleBySymbolicName(symbolicName);
                response.put("success", bundle != null);
                response.put("data", bundle);
                
            } else if (requestData.startsWith("devtools:bundles.reload:")) {
                String symbolicName = requestData.substring("devtools:bundles.reload:".length());
                try {
                    bundleService.reloadBundleFromFileSystem(symbolicName);
                    response.put("success", true);
                    response.put("message", "Bundle reloaded: " + symbolicName);
                } catch (Exception e) {
                    response.put("success", false);
                    response.put("error", e.getMessage());
                }
                
            } else if (requestData.startsWith("devtools:bundles.start:")) {
                try {
                    long bundleId = Long.parseLong(requestData.substring("devtools:bundles.start:".length()));
                    bundleService.startBundle(bundleId);
                    response.put("success", true);
                    response.put("message", "Bundle started");
                } catch (Exception e) {
                    response.put("success", false);
                    response.put("error", e.getMessage());
                }
                
            } else if (requestData.startsWith("devtools:bundles.stop:")) {
                try {
                    long bundleId = Long.parseLong(requestData.substring("devtools:bundles.stop:".length()));
                    bundleService.stopBundle(bundleId);
                    response.put("success", true);
                    response.put("message", "Bundle stopped");
                } catch (Exception e) {
                    response.put("success", false);
                    response.put("error", e.getMessage());
                }
                
            } else if (requestData.startsWith("devtools:bundles.restart:")) {
                try {
                    long bundleId = Long.parseLong(requestData.substring("devtools:bundles.restart:".length()));
                    bundleService.restartBundle(bundleId);
                    response.put("success", true);
                    response.put("message", "Bundle restarted");
                } catch (Exception e) {
                    response.put("success", false);
                    response.put("error", e.getMessage());
                }
                
            } else if (requestData.startsWith("devtools:bundles.dependencies:")) {
                try {
                    long bundleId = Long.parseLong(requestData.substring("devtools:bundles.dependencies:".length()));
                    Map<String, Object> deps = bundleService.getBundleDependencies(bundleId);
                    response.put("success", true);
                    response.put("data", deps);
                } catch (Exception e) {
                    response.put("success", false);
                    response.put("error", e.getMessage());
                }
                
            } else if (requestData.startsWith("devtools:bundles.devStatus:")) {
                String symbolicName = requestData.substring("devtools:bundles.devStatus:".length());
                Map<String, Object> status = bundleService.getDevelopmentStatus(symbolicName);
                response.put("success", status != null);
                response.put("data", status);
                
            } else if ("devtools:services.list".equals(requestData)) {
                response.put("success", true);
                response.put("data", serviceService.listServices());
                
            } else if (requestData.startsWith("devtools:services.find:")) {
                String interfaceName = requestData.substring("devtools:services.find:".length());
                response.put("success", true);
                response.put("data", serviceService.findServicesByInterface(interfaceName));
                
            } else if (requestData.startsWith("devtools:services.get:")) {
                try {
                    long serviceId = Long.parseLong(requestData.substring("devtools:services.get:".length()));
                    Map<String, Object> service = serviceService.getServiceProperties(serviceId);
                    response.put("success", service != null);
                    response.put("data", service);
                } catch (Exception e) {
                    response.put("success", false);
                    response.put("error", e.getMessage());
                }
                
            } else if ("devtools:metrics".equals(requestData)) {
                response.put("success", true);
                response.put("data", metricsService.getAllMetrics());
                
            } else if ("devtools:logs.files".equals(requestData)) {
                response.put("success", true);
                response.put("data", logService.getLogFiles());
                
            } else if (requestData.startsWith("devtools:logs.read:")) {
                // Format: devtools:logs.read:filePath:maxLines:level:searchTerm
                String[] parts = requestData.substring("devtools:logs.read:".length()).split(":", 4);
                String filePath = parts.length > 0 && !parts[0].isEmpty() ? parts[0] : null;
                int maxLines = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) : 100;
                String level = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : "ALL";
                String searchTerm = parts.length > 3 && !parts[3].isEmpty() ? parts[3] : null;
                
                if (filePath != null) {
                    try {
                        response.put("success", true);
                        response.put("data", logService.readLogEntries(filePath, maxLines, level, searchTerm));
                    } catch (Exception e) {
                        response.put("success", false);
                        response.put("error", e.getMessage());
                    }
                } else {
                    response.put("success", false);
                    response.put("error", "File path required");
                }
                
            } else {
                response.put("success", false);
                response.put("error", "Unknown request: " + requestData);
            }
            
            return Mono.just(DefaultPayload.create(mapper.writeValueAsString(response)));
            
        } catch (JsonProcessingException e) {
            logger.error("Error processing request", e);
            try {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", e.getMessage());
                return Mono.just(DefaultPayload.create(mapper.writeValueAsString(error)));
            } catch (JsonProcessingException e2) {
                return Mono.just(DefaultPayload.create("{\"success\":false,\"error\":\"Serialization error\"}"));
            }
        }
    }
    
    /**
     * Handle request-stream requests.
     */
    private Flux<Payload> handleRequestStream(Payload payload) {
        String requestData = payload.getDataUtf8();
        logger.debug("Received devtools stream request: {}", requestData);
        
        try {
            if ("devtools:stream:bundles".equals(requestData)) {
                return bundleEventSink.asFlux()
                    .map(event -> {
                        try {
                            return DefaultPayload.create(mapper.writeValueAsString(event));
                        } catch (JsonProcessingException e) {
                            logger.error("Error serializing bundle event", e);
                            return DefaultPayload.create("{}");
                        }
                    });
                    
            } else if ("devtools:stream:metrics".equals(requestData)) {
                return metricsEventSink.asFlux()
                    .map(event -> {
                        try {
                            return DefaultPayload.create(mapper.writeValueAsString(event));
                        } catch (JsonProcessingException e) {
                            logger.error("Error serializing metrics event", e);
                            return DefaultPayload.create("{}");
                        }
                    });
                    
            } else if ("devtools:stream:logs".equals(requestData)) {
                return logService.getLogEventSink().asFlux()
                    .map(event -> {
                        try {
                            return DefaultPayload.create(mapper.writeValueAsString(event));
                        } catch (JsonProcessingException e) {
                            logger.error("Error serializing log event", e);
                            return DefaultPayload.create("{}");
                        }
                    });
            }
            
            return Flux.error(new IllegalArgumentException("Unknown stream: " + requestData));
            
        } catch (Exception e) {
            logger.error("Error handling stream request", e);
            return Flux.error(e);
        }
    }
    
    /**
     * Start metrics streaming (poll every 2 seconds).
     */
    private void startMetricsStream() {
        Flux.interval(Duration.ofSeconds(2))
            .map(tick -> metricsService.getAllMetrics())
            .subscribe(metrics -> {
                Map<String, Object> event = new HashMap<>();
                event.put("type", "metrics");
                event.put("data", metrics);
                event.put("timestamp", System.currentTimeMillis());
                metricsEventSink.tryEmitNext(event);
            });
    }
    
    /**
     * Emit bundle event to stream.
     */
    public void emitBundleEvent(Map<String, Object> event) {
        if (bundleEventSink != null) {
            Sinks.EmitResult result = bundleEventSink.tryEmitNext(event);
            if (result.isFailure()) {
                logger.debug("Failed to emit bundle event: {}", result);
            }
        }
    }
}
