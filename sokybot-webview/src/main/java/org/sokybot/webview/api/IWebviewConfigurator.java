package org.sokybot.webview.api;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import reactor.core.publisher.Flux;

/**
 * Extension API for bundles to register declarative UI pages with the webview.
 * 
 * Bundles can use this interface to:
 * - Register UI pages defined in JSON schemas
 * - Handle user actions from the UI
 * - Provide real-time data streams
 * - Send events to the frontend
 */
public interface IWebviewConfigurator {
    
    /**
     * Register a declarative page using a JSON schema.
     * 
     * @param pageId Unique identifier for the page
     * @param title Display title for the page
     * @param iconPath Path to icon (SVG/PNG) or icon name
     * @param schema UI schema as Map (typically loaded from JSON)
     */
    void addDeclarativePage(String pageId, String title, String iconPath, Map<String, Object> schema);
    
    /**
     * Register a handler that provides schema and data for a page.
     * Called when frontend requests page data.
     * 
     * @param pageId Page identifier
     * @param handler Function that takes request string and returns schema/data map
     */
    void registerSchemaHandler(String pageId, Function<String, Map<String, Object>> handler);
    
    /**
     * Register a handler for user actions from the UI.
     * 
     * @param pageId Page identifier
     * @param handler Function that takes action name and data, returns response with state updates
     */
    void registerActionHandler(String pageId, BiFunction<String, Map<String, Object>, Map<String, Object>> handler);
    
    /**
     * Register a stream handler for continuous data updates.
     * 
     * @param pageId Page identifier
     * @param streamId Unique stream identifier within the page
     * @param handler Function that takes parameters and returns a Flux of data
     */
    void registerStreamHandler(String pageId, String streamId, 
                               Function<Map<String, Object>, Flux<Map<String, Object>>> handler);
    
    /**
     * Register a stream handler with automatic state updates.
     * 
     * @param pageId Page identifier
     * @param streamId Stream identifier
     * @param handler Stream handler function
     * @param stateKey Key in state object to update with stream data
     */
    void registerStreamHandler(String pageId, String streamId,
                               Function<Map<String, Object>, Flux<Map<String, Object>>> handler,
                               String stateKey);
    
    /**
     * Send an event to the frontend.
     * 
     * @param eventType Event type identifier
     * @param data Event data
     */
    void sendEvent(String eventType, Map<String, Object> data);
    
    /**
     * Remove a registered page.
     * 
     * @param pageId Page identifier to remove
     */
    void removePage(String pageId);
    
    // ==================== Toolbar Actions ====================
    
    /**
     * Register a toolbar action button that appears in the header.
     * When clicked, the modal defined by modalSchema will be displayed.
     * 
     * @param actionId Unique identifier for the action
     * @param title Tooltip text for the button
     * @param iconName Lucide icon name (e.g., "Package", "Settings", "Cog")
     * @param modalSchema UI schema for the modal dialog (same format as page schema)
     */
    void addToolbarAction(String actionId, String title, String iconName, Map<String, Object> modalSchema);
    
    /**
     * Remove a toolbar action.
     * 
     * @param actionId Action identifier to remove
     */
    void removeToolbarAction(String actionId);
    
    /**
     * Register a handler for actions triggered from within a toolbar action's modal.
     * 
     * @param actionId Toolbar action identifier
     * @param handler Function that takes action name and data, returns response
     */
    void registerToolbarActionHandler(String actionId, BiFunction<String, Map<String, Object>, Map<String, Object>> handler);
    
    /**
     * Register a stream handler for a toolbar action's modal.
     * 
     * @param actionId Toolbar action identifier
     * @param streamId Stream identifier
     * @param handler Stream handler function
     */
    void registerToolbarStreamHandler(String actionId, String streamId,
                                       Function<Map<String, Object>, Flux<Map<String, Object>>> handler);
}
