package org.sokybot.webview.handler;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.webview.WebviewConfigurator;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Mono;

/**
 * Handler for extension-related operations.
 * 
 * Methods:
 *   - extension.schema: Get schema for an extension page
 *   - extension.action: Trigger an action on an extension page
 *   - extension.registry: Get all registered extensions
 *   - extension.toolbar.action: Trigger a toolbar action
 */
@Component(
    service = IRSocketHandler.class,
    property = {
        IRSocketHandler.METHOD_PROPERTY + "=extension.schema",
        IRSocketHandler.METHOD_PROPERTY + "=extension.action",
        IRSocketHandler.METHOD_PROPERTY + "=extension.registry",
        IRSocketHandler.METHOD_PROPERTY + "=extension.toolbar.action"
    }
)
public class ExtensionHandler implements IRSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ExtensionHandler.class);
    
    private volatile WebviewConfigurator extensionConfigurator;
    
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setExtensionConfigurator(WebviewConfigurator configurator) {
        this.extensionConfigurator = configurator;
    }
    
    protected void unsetExtensionConfigurator(WebviewConfigurator configurator) {
        this.extensionConfigurator = null;
    }
    
    @Override
    public String getMethod() {
        return "extension.*";
    }
    
    @Override
    public String getDescription() {
        return "Extension management (schema, action, registry, toolbar)";
    }
    
    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        String method = request.getMethod();
        
        switch (method) {
            case "extension.schema":
                return handleSchema(request);
            case "extension.action":
                return handleAction(request);
            case "extension.registry":
                return handleRegistry(request);
            case "extension.toolbar.action":
                return handleToolbarAction(request);
            default:
                return Mono.just(RSocketResponse.methodNotFound(method));
        }
    }
    
    private Mono<RSocketResponse> handleSchema(RSocketRequest request) {
        String pageId = request.getString("pageId");
        String machineId = request.getString("machineId");
        
        if (pageId == null || pageId.isEmpty()) {
            return Mono.just(RSocketResponse.invalidParams("pageId is required"));
        }
        
        if (extensionConfigurator == null) {
            return Mono.just(RSocketResponse.success(new HashMap<>()));
        }
        
        String requestStr = machineId != null ? machineId : "";
        Map<String, Object> response = extensionConfigurator.handleSchemaRequest(pageId, requestStr);
        return Mono.just(RSocketResponse.success(response));
    }
    
    @SuppressWarnings("unchecked")
    private Mono<RSocketResponse> handleAction(RSocketRequest request) {
        String pageId = request.getString("pageId");
        String action = request.getString("action");
        Map<String, Object> data = request.get("data", Map.class);
        
        if (pageId == null || pageId.isEmpty()) {
            return Mono.just(RSocketResponse.invalidParams("pageId is required"));
        }
        if (action == null || action.isEmpty()) {
            return Mono.just(RSocketResponse.invalidParams("action is required"));
        }
        
        if (extensionConfigurator == null) {
            return Mono.just(RSocketResponse.error(
                RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                "Extension configurator not available"
            ));
        }
        
        try {
            Map<String, Object> response = extensionConfigurator.handleAction(
                pageId, action, data != null ? data : new HashMap<>()
            );
            return Mono.just(RSocketResponse.success(response));
        } catch (Exception e) {
            logger.error("Error handling extension action: {}", action, e);
            return Mono.just(RSocketResponse.internalError(e));
        }
    }
    
    private Mono<RSocketResponse> handleRegistry(RSocketRequest request) {
        if (extensionConfigurator == null) {
            return Mono.just(RSocketResponse.success(new HashMap<>()));
        }
        
        Map<String, Object> registry = extensionConfigurator.getExtensionRegistry();
        return Mono.just(RSocketResponse.success(registry));
    }
    
    @SuppressWarnings("unchecked")
    private Mono<RSocketResponse> handleToolbarAction(RSocketRequest request) {
        String actionId = request.getString("actionId");
        String action = request.getString("action");
        Map<String, Object> data = request.get("data", Map.class);
        
        if (actionId == null || actionId.isEmpty()) {
            return Mono.just(RSocketResponse.invalidParams("actionId is required"));
        }
        if (action == null || action.isEmpty()) {
            return Mono.just(RSocketResponse.invalidParams("action is required"));
        }
        
        if (extensionConfigurator == null) {
            return Mono.just(RSocketResponse.error(
                RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                "Extension configurator not available"
            ));
        }
        
        try {
            Map<String, Object> response = extensionConfigurator.handleToolbarAction(
                actionId, action, data != null ? data : new HashMap<>()
            );
            return Mono.just(RSocketResponse.success(response));
        } catch (Exception e) {
            logger.error("Error handling toolbar action: {}", action, e);
            return Mono.just(RSocketResponse.internalError(e));
        }
    }
}
