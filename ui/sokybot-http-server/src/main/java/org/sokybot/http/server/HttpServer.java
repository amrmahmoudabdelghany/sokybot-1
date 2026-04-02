package org.sokybot.http.server;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.http.server.api.IWebSocketHandler;
import org.sokybot.http.server.api.IWebSocketRegistry;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Shared HTTP server based on Reactor Netty.
 * Provides HTTP routing and WebSocket handling, with Dev Mode Proxy
 * capabilities including HMR WebSocket proxying.
 */
@Component(service = { IWebSocketRegistry.class }, immediate = true, scope = ServiceScope.SINGLETON)
@Designate(ocd = HttpServerConfig.class)
public class HttpServer implements IWebSocketRegistry {

    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private DisposableServer server;
    private int port;
    private boolean devMode;
    private String webviewDevUrl;
    private String devtoolsDevUrl;
    private String staticRoot;

    /** Registered WebSocket handlers by path */
    private final ConcurrentMap<String, IWebSocketHandler> wsHandlers = new ConcurrentHashMap<>();

    @Activate
    public void activate(HttpServerConfig config) {
        // Prevent multiple activations
        if (this.server != null) {
            logger.debug("HTTP Server already activated, skipping duplicate activation");
            return;
        }

        this.port = config.port();
        this.devMode = config.devMode();
        this.webviewDevUrl = config.webviewDevUrl();
        this.devtoolsDevUrl = config.devtoolsDevUrl();
        this.staticRoot = trimRoot(config.staticRoot());

        logger.info("Starting Sokybot HTTP Server on port {} (DevMode: {})", port, devMode);

        try {
            reactor.netty.http.server.HttpServer httpServer = reactor.netty.http.server.HttpServer.create()
                    .host(config.host())
                    .port(port)
                    .route(this::configureRoutes);

            this.server = httpServer.bindNow();
            logger.info("Sokybot HTTP Server started successfully on port {}", port);
        } catch (Exception e) {
            logger.error("Failed to start HTTP Server on port {}: {}", port, e.getMessage());
            throw new RuntimeException("Failed to start HTTP Server", e);
        }
    }

    private void configureRoutes(HttpServerRoutes routes) {
        routes.route(req -> true, this::handleRequest);
    }

    private Mono<Void> handleRequest(HttpServerRequest req, HttpServerResponse res) {
        String path = req.uri();

        // 1. WebSocket Handling
        if (req.requestHeaders().contains(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true)) {
            return handleWebSocket(req, res, path);
        }

        // 2. HTTP Proxying (Dev Mode)
        if (devMode) {
            return proxyHttpRequest(req, res, path);
        }

        // 3. Static File Serving (Prod Mode)
        return serveStaticRequest(req, res, path);
    }

    private Mono<Void> serveStaticRequest(HttpServerRequest req, HttpServerResponse res, String rawPath) {
        String path = stripQuery(rawPath);
        String normalizedPath = normalizePath(path);
        String resourcePath = staticRoot + normalizedPath;
        byte[] bytes = readResourceBytes(resourcePath);
        if (bytes != null) {
            res.header("Content-Type", contentTypeFor(normalizedPath));
            return res.send(Mono.just(Unpooled.wrappedBuffer(bytes))).then();
        }
        if (isSpaFallbackRequest(req, normalizedPath)) {
            byte[] index = readResourceBytes(staticRoot + "/index.html");
            if (index != null) {
                res.header("Content-Type", "text/html; charset=utf-8");
                return res.send(Mono.just(Unpooled.wrappedBuffer(index))).then();
            }
        }
        return res.status(404).sendString(Mono.just("Not Found")).then();
    }

    private boolean isSpaFallbackRequest(HttpServerRequest req, String normalizedPath) {
        if (!HttpMethod.GET.name().equalsIgnoreCase(req.method().name())) return false;
        if (normalizedPath.startsWith("/rsocket") || normalizedPath.startsWith("/api/")) return false;
        String accept = req.requestHeaders().get(HttpHeaderNames.ACCEPT);
        return accept != null && accept.toLowerCase(Locale.ROOT).contains("text/html");
    }

    private byte[] readResourceBytes(String resourcePath) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            return in.readAllBytes();
        } catch (Exception e) {
            logger.debug("Failed to read static resource {}", resourcePath, e);
            return null;
        }
    }

    private static String stripQuery(String path) {
        int q = path.indexOf('?');
        return q >= 0 ? path.substring(0, q) : path;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) return "/index.html";
        if (!path.startsWith("/")) return "/" + path;
        return path;
    }

    private static String trimRoot(String root) {
        if (root == null || root.isEmpty()) return "webapp";
        String value = root.startsWith("/") ? root.substring(1) : root;
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value.isEmpty() ? "webapp" : value;
    }

    private static String contentTypeFor(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html")) return "text/html; charset=utf-8";
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "application/javascript; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }

    private Mono<Void> handleWebSocket(HttpServerRequest req, HttpServerResponse res, String path) {
        // Extract path without query string for handler lookup
        String pathWithoutQuery = path.contains("?") ? path.substring(0, path.indexOf("?")) : path;

        // Check for registered application WebSocket handlers first
        IWebSocketHandler handler = wsHandlers.get(pathWithoutQuery);
        if (handler != null) {
            logger.debug("WebSocket connection accepted for path: {} (registered: {})", path, pathWithoutQuery);
            return res.sendWebsocket((in, out) -> handler.handle(in, out));
        }

        // In dev mode, proxy WebSocket to Vite for HMR
        if (devMode) {
            return proxyWebSocket(req, res, path);
        }

        return res.status(404).send();
    }

    private Mono<Void> proxyWebSocket(HttpServerRequest req, HttpServerResponse res, String path) {
        String targetUrl = webviewDevUrl;
        if (path.startsWith("/devtools")) {
            targetUrl = devtoolsDevUrl;
        }

        // Convert HTTP URL to WebSocket URL
        String wsUrl = targetUrl.replace("http://", "ws://").replace("https://", "wss://");
        String queryString = req.uri().contains("?") ? req.uri().substring(req.uri().indexOf("?")) : "";
        String fullWsUrl = wsUrl + path + queryString;

        logger.debug("Proxying WebSocket {} to {}", path, fullWsUrl);

        return res.sendWebsocket((clientIn, clientOut) -> {
            // Create a sink to relay messages from upstream to client
            Sinks.Many<WebSocketFrame> toClientSink = Sinks.many().unicast().onBackpressureBuffer();

            // Connect to upstream Vite server
            HttpClient.create()
                    .websocket()
                    .uri(fullWsUrl)
                    .handle((upstreamIn, upstreamOut) -> {
                        // Relay messages from upstream to client
                        upstreamIn.receiveFrames()
                                .doOnNext(frame -> {
                                    WebSocketFrame copy = frame.copy();
                                    toClientSink.tryEmitNext(copy);
                                })
                                .doOnComplete(() -> toClientSink.tryEmitComplete())
                                .doOnError(e -> toClientSink.tryEmitError(e))
                                .subscribe();

                        // Relay messages from client to upstream
                        return upstreamOut.sendObject(
                                clientIn.receiveFrames()
                                        .map(frame -> frame.copy()))
                                .then();
                    })
                    .subscribe();

            // Send relayed messages to client
            return clientOut.sendObject(toClientSink.asFlux()).then();
        });
    }

    private Mono<Void> proxyHttpRequest(HttpServerRequest req, HttpServerResponse res, String path) {
        String targetUrl = webviewDevUrl;
        if (path.startsWith("/devtools")) {
            targetUrl = devtoolsDevUrl;
        }

        String finalUri = targetUrl + path;

        // Use streaming proxy for better performance
        HttpClient client = HttpClient.create()
                .headers(h -> {
                    // Copy request headers
                    req.requestHeaders().forEach(entry -> {
                        if (!entry.getKey().equalsIgnoreCase("host")) {
                            h.set(entry.getKey(), entry.getValue());
                        }
                    });
                });

        return client.request(req.method())
                .uri(finalUri)
                .send(req.receive().retain())
                .responseSingle((upstreamRes, upstreamBody) -> {
                    // Set response status
                    res.status(upstreamRes.status());

                    // Copy response headers (filter out problematic ones)
                    upstreamRes.responseHeaders().forEach(entry -> {
                        String headerName = entry.getKey().toLowerCase();
                        if (!headerName.equals("transfer-encoding")
                                && !headerName.equals("connection")) {
                            res.header(entry.getKey(), entry.getValue());
                        }
                    });

                    // Stream the response body
                    return upstreamBody
                            .defaultIfEmpty(io.netty.buffer.Unpooled.EMPTY_BUFFER)
                            .flatMap(body -> res.send(Mono.just(body.retain())).then());
                });
    }

    @Deactivate
    public void deactivate() {
        logger.info("Stopping Sokybot HTTP Server");
        wsHandlers.clear();
        if (server != null) {
            server.disposeNow();
        }
    }

    // ========== IWebSocketRegistry Implementation ==========

    @Override
    public void registerHandler(String path, IWebSocketHandler handler) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
        if (handler != null) {
            wsHandlers.put(path, handler);
            logger.info("WebSocket handler registered for path: {}", path);
        }
    }

    @Override
    public void unregisterHandler(String path) {
        if (path != null) {
            wsHandlers.remove(path);
        }
    }

    @Override
    public boolean hasHandler(String path) {
        return path != null && wsHandlers.containsKey(path);
    }

    @Override
    public int getPort() {
        return port;
    }
}
