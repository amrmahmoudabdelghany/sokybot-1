package org.sokybot.webview;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import org.sokybot.webview.api.IWebviewConfigurator;

/**
 * Implementation of IWebviewConfigurator that manages extension pages,
 * action handlers, stream handlers, and toolbar actions.
 */
@Component(service = IWebviewConfigurator.class, immediate = true)
public class WebviewConfigurator implements IWebviewConfigurator {
    
    private static final Logger logger = LoggerFactory.getLogger(WebviewConfigurator.class);
    
    private RSocketServerService rsocketService;
    
    // Registry for page schemas
    private final Map<String, PageRegistration> pageRegistrations = new ConcurrentHashMap<>();
    
    // Registry for schema handlers
    private final Map<String, Function<String, Map<String, Object>>> schemaHandlers = new ConcurrentHashMap<>();
    
    // Registry for action handlers
    private final Map<String, BiFunction<String, Map<String, Object>, Map<String, Object>>> actionHandlers = new ConcurrentHashMap<>();
    
    // Registry for stream handlers
    private final Map<String, StreamRegistration> streamHandlers = new ConcurrentHashMap<>();
    
    // Registry for toolbar actions
    private final Map<String, ToolbarActionRegistration> toolbarActions = new ConcurrentHashMap<>();
    
    // Registry for toolbar action handlers
    private final Map<String, BiFunction<String, Map<String, Object>, Map<String, Object>>> toolbarActionHandlers = new ConcurrentHashMap<>();
    
    @Reference
    public void setRSocketService(RSocketServerService service) {
        this.rsocketService = service;
        if (service != null) {
            service.registerExtensionConfigurator(this);
        }
    }
    
    @Activate
    public void activate() {
        logger.info("WebviewConfigurator activated");
    }
    
    @Override
    public void addDeclarativePage(String pageId, String title, String iconPath, Map<String, Object> schema) {
        logger.info("Registering declarative page: {}", pageId);
        PageRegistration registration = new PageRegistration(pageId, title, iconPath, schema);
        pageRegistrations.put(pageId, registration);
        
        // Notify frontend via RSocket event
        Map<String, Object> event = new HashMap<>();
        event.put("type", "extension.page.added");
        event.put("pageId", pageId);
        event.put("title", title);
        event.put("iconPath", iconPath);
        event.put("schema", schema);
        sendEvent("ui.extension", event);
    }
    
    @Override
    public void registerSchemaHandler(String pageId, Function<String, Map<String, Object>> handler) {
        logger.debug("Registering schema handler for page: {}", pageId);
        schemaHandlers.put(pageId, handler);
    }
    
    @Override
    public void registerActionHandler(String pageId, BiFunction<String, Map<String, Object>, Map<String, Object>> handler) {
        logger.debug("Registering action handler for page: {}", pageId);
        actionHandlers.put(pageId, handler);
    }
    
    @Override
    public void registerStreamHandler(String pageId, String streamId,
            Function<Map<String, Object>, Flux<Map<String, Object>>> handler) {
        registerStreamHandler(pageId, streamId, handler, null);
    }
    
    @Override
    public void registerStreamHandler(String pageId, String streamId,
            Function<Map<String, Object>, Flux<Map<String, Object>>> handler,
            String stateKey) {
        logger.debug("Registering stream handler for page: {}, stream: {}", pageId, streamId);
        String key = pageId + ":" + streamId;
        StreamRegistration registration = new StreamRegistration(handler, stateKey);
        streamHandlers.put(key, registration);
        
        if (rsocketService != null) {
            rsocketService.registerStreamHandler(key, handler);
        }
    }
    
    @Override
    public void sendEvent(String eventType, Map<String, Object> data) {
        if (rsocketService != null) {
            rsocketService.sendExtensionEvent(eventType, data);
        }
    }
    
    @Override
    public void removePage(String pageId) {
        logger.info("Removing page: {}", pageId);
        pageRegistrations.remove(pageId);
        schemaHandlers.remove(pageId);
        actionHandlers.remove(pageId);
        
        // Remove all stream handlers for this page
        streamHandlers.entrySet().removeIf(entry -> entry.getKey().startsWith(pageId + ":"));
        
        // Notify frontend
        Map<String, Object> event = new HashMap<>();
        event.put("type", "extension.page.removed");
        event.put("pageId", pageId);
        sendEvent("ui.extension", event);
    }
    
    // ==================== Toolbar Actions ====================
    
    @Override
    public void addToolbarAction(String actionId, String title, String iconName, Map<String, Object> modalSchema) {
        logger.info("Registering toolbar action: {}", actionId);
        ToolbarActionRegistration registration = new ToolbarActionRegistration(actionId, title, iconName, modalSchema);
        toolbarActions.put(actionId, registration);
        
        // Notify frontend
        Map<String, Object> event = new HashMap<>();
        event.put("type", "extension.toolbar.added");
        event.put("actionId", actionId);
        event.put("title", title);
        event.put("iconName", iconName);
        event.put("modalSchema", modalSchema);
        sendEvent("ui.extension", event);
    }
    
    @Override
    public void removeToolbarAction(String actionId) {
        logger.info("Removing toolbar action: {}", actionId);
        toolbarActions.remove(actionId);
        toolbarActionHandlers.remove(actionId);
        
        // Remove stream handlers for this toolbar action
        streamHandlers.entrySet().removeIf(entry -> entry.getKey().startsWith("toolbar:" + actionId + ":"));
        
        // Notify frontend
        Map<String, Object> event = new HashMap<>();
        event.put("type", "extension.toolbar.removed");
        event.put("actionId", actionId);
        sendEvent("ui.extension", event);
    }
    
    @Override
    public void registerToolbarActionHandler(String actionId, BiFunction<String, Map<String, Object>, Map<String, Object>> handler) {
        logger.debug("Registering toolbar action handler for: {}", actionId);
        toolbarActionHandlers.put(actionId, handler);
    }
    
    @Override
    public void registerToolbarStreamHandler(String actionId, String streamId,
            Function<Map<String, Object>, Flux<Map<String, Object>>> handler) {
        logger.debug("Registering toolbar stream handler for action: {}, stream: {}", actionId, streamId);
        String key = "toolbar:" + actionId + ":" + streamId;
        StreamRegistration registration = new StreamRegistration(handler, null);
        streamHandlers.put(key, registration);
        
        if (rsocketService != null) {
            rsocketService.registerStreamHandler(key, handler);
        }
    }
    
    // Internal methods for RSocket service
    
    public Map<String, Object> handleSchemaRequest(String pageId, String request) {
        Function<String, Map<String, Object>> handler = schemaHandlers.get(pageId);
        if (handler != null) {
            return handler.apply(request);
        }
        // Return page schema if no handler
        PageRegistration registration = pageRegistrations.get(pageId);
        if (registration != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("schema", registration.schema);
            return response;
        }
        return new HashMap<>();
    }
    
    public Map<String, Object> handleAction(String pageId, String action, Map<String, Object> data) {
        BiFunction<String, Map<String, Object>, Map<String, Object>> handler = actionHandlers.get(pageId);
        if (handler != null) {
            return handler.apply(action, data);
        }
        logger.warn("No action handler found for page: {}, action: {}", pageId, action);
        return Map.of("success", false, "error", "No handler found for action: " + action);
    }
    
    public Map<String, Object> handleToolbarAction(String actionId, String action, Map<String, Object> data) {
        BiFunction<String, Map<String, Object>, Map<String, Object>> handler = toolbarActionHandlers.get(actionId);
        if (handler != null) {
            return handler.apply(action, data);
        }
        logger.warn("No handler found for toolbar action: {}, action: {}", actionId, action);
        return Map.of("success", false, "error", "No handler found for toolbar action: " + action);
    }
    
    public Map<String, Object> getExtensionRegistry() {
        Map<String, Object> registry = new HashMap<>();
        
        // Pages
        Map<String, Object> pages = new HashMap<>();
        for (PageRegistration reg : pageRegistrations.values()) {
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("pageId", reg.pageId);
            pageInfo.put("title", reg.title);
            pageInfo.put("iconPath", reg.iconPath);
            pageInfo.put("schema", reg.schema);
            pages.put(reg.pageId, pageInfo);
        }
        registry.put("pages", pages);
        
        // Toolbar Actions
        Map<String, Object> toolbarActionsMap = new HashMap<>();
        for (ToolbarActionRegistration reg : toolbarActions.values()) {
            Map<String, Object> actionInfo = new HashMap<>();
            actionInfo.put("actionId", reg.actionId);
            actionInfo.put("title", reg.title);
            actionInfo.put("iconName", reg.iconName);
            actionInfo.put("modalSchema", reg.modalSchema);
            toolbarActionsMap.put(reg.actionId, actionInfo);
        }
        registry.put("toolbarActions", toolbarActionsMap);
        
        return registry;
    }
    
    // Internal data classes
    
    private static class PageRegistration {
        final String pageId;
        final String title;
        final String iconPath;
        final Map<String, Object> schema;
        
        PageRegistration(String pageId, String title, String iconPath, Map<String, Object> schema) {
            this.pageId = pageId;
            this.title = title;
            this.iconPath = iconPath;
            this.schema = schema;
        }
    }
    
    private static class StreamRegistration {
        final Function<Map<String, Object>, Flux<Map<String, Object>>> handler;
        final String stateKey;
        
        StreamRegistration(Function<Map<String, Object>, Flux<Map<String, Object>>> handler, String stateKey) {
            this.handler = handler;
            this.stateKey = stateKey;
        }
    }
    
    private static class ToolbarActionRegistration {
        final String actionId;
        final String title;
        final String iconName;
        final Map<String, Object> modalSchema;
        
        ToolbarActionRegistration(String actionId, String title, String iconName, Map<String, Object> modalSchema) {
            this.actionId = actionId;
            this.title = title;
            this.iconName = iconName;
            this.modalSchema = modalSchema;
        }
    }
}
