package org.sokybot.http.server.api;

/**
 * Service interface for registering WebSocket handlers at specific paths.
 * This enables OSGi bundles to dynamically add WebSocket endpoints to the
 * shared HTTP server without tight coupling.
 * 
 * <p>Example usage:</p>
 * <pre>
 * &#64;Reference
 * private IWebSocketRegistry wsRegistry;
 * 
 * &#64;Activate
 * public void start() {
 *     wsRegistry.registerHandler("/my-websocket", ws -> {
 *         ws.textMessageHandler(msg -> ws.writeTextMessage("Echo: " + msg));
 *     });
 * }
 * 
 * &#64;Deactivate
 * public void stop() {
 *     wsRegistry.unregisterHandler("/my-websocket");
 * }
 * </pre>
 */
public interface IWebSocketRegistry {
    
    /**
     * Register a WebSocket handler at the specified path.
     * When a WebSocket upgrade request is received for this path,
     * the handler will be invoked with the WebSocket connection.
     * 
     * @param path the URL path to handle (e.g., "/rsocket", "/devtools-ws")
     * @param handler the handler to process WebSocket connections
     * @throws IllegalArgumentException if path is null or empty
     * @throws IllegalStateException if a handler is already registered for this path
     */
    void registerHandler(String path, IWebSocketHandler handler);
    
    /**
     * Unregister a previously registered WebSocket handler.
     * After this call, WebSocket connections to the path will be rejected.
     * 
     * @param path the URL path to unregister
     */
    void unregisterHandler(String path);
    
    /**
     * Check if a handler is registered for the given path.
     * 
     * @param path the URL path to check
     * @return true if a handler is registered, false otherwise
     */
    boolean hasHandler(String path);
    
    /**
     * Get the port the HTTP server is listening on.
     * Useful for clients that need to construct WebSocket URLs.
     * 
     * @return the HTTP server port
     */
    int getPort();
}
