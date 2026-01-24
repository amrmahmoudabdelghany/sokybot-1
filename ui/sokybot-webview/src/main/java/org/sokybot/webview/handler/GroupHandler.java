package org.sokybot.webview.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Mono;

/**
 * Handler for group management operations.
 * 
 * Methods:
 *   - group.list: List all groups
 *   - group.details: Get group details
 *   - group.create: Create a new group
 */
@Component(
    service = IRSocketHandler.class,
    property = {
        IRSocketHandler.METHOD_PROPERTY + "=group.list",
        IRSocketHandler.METHOD_PROPERTY + "=group.details",
        IRSocketHandler.METHOD_PROPERTY + "=group.create"
    }
)
public class GroupHandler implements IRSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GroupHandler.class);
    
    private volatile ISokybotContext sokybotContext;
    
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setSokybotContext(ISokybotContext sokybotContext) {
        this.sokybotContext = sokybotContext;
    }
    
    protected void unsetSokybotContext(ISokybotContext sokybotContext) {
        this.sokybotContext = null;
    }
    
    @Override
    public String[] getMethods() {
        return new String[] { "group.list", "group.details", "group.create" };
    }
    
    @Override
    public String getDescription() {
        return "Group management operations (list, details, create)";
    }
    
    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        String method = request.getMethod();
        
        switch (method) {
            case "group.list":
                return handleList(request);
            case "group.details":
                return handleDetails(request);
            case "group.create":
                return handleCreate(request);
            default:
                return Mono.just(RSocketResponse.methodNotFound(method));
        }
    }
    
    private Mono<RSocketResponse> handleList(RSocketRequest request) {
        if (sokybotContext == null) {
            return Mono.just(RSocketResponse.success(new ArrayList<>()));
        }
        
        List<Map<String, Object>> groups = new ArrayList<>();
        for (String name : sokybotContext.listNames()) {
            Map<String, Object> groupInfo = new HashMap<>();
            groupInfo.put("name", name);
            
            IGroupContext grpCtx = sokybotContext.findGroupCtx(name).orElse(null);
            if (grpCtx != null) {
                groupInfo.put("machineCount", grpCtx.getMachines().length);
            }
            groups.add(groupInfo);
        }
        
        return Mono.just(RSocketResponse.success(groups));
    }
    
    private Mono<RSocketResponse> handleDetails(RSocketRequest request) {
        String groupName = request.getString("name");
        
        if (groupName == null || groupName.isEmpty()) {
            return Mono.just(RSocketResponse.invalidParams("name is required"));
        }
        
        if (sokybotContext == null) {
            return Mono.just(RSocketResponse.error(
                RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                "Application not ready"
            ));
        }
        
        IGroupContext grpCtx = sokybotContext.findGroupCtx(groupName).orElse(null);
        if (grpCtx == null) {
            return Mono.just(RSocketResponse.notFound("Group not found: " + groupName));
        }
        
        try {
            IGameDataLookup lookup = grpCtx.getGameDataLookup();
            if (lookup == null) {
                logger.error("GameDataLookup is null for group: {}. PersistenceFactory may not be available.", groupName);
                return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Game data not loaded. Check that PK2 files exist."
                ));
            }
            
            Map<String, Object> details = new HashMap<>();
            details.put("name", groupName);
            details.put("version", lookup.getVersion());
            
            Map<String, List<String>> divHosts = lookup.getDivHosts();
            details.put("hosts", divHosts != null ? divHosts : new HashMap<>());
            details.put("machineCount", grpCtx.getMachines().length);
            
            return Mono.just(RSocketResponse.success(details));
        } catch (Exception e) {
            logger.error("Error loading game details for group: {}", groupName, e);
            return Mono.just(RSocketResponse.internalError(e));
        }
    }
    
    private Mono<RSocketResponse> handleCreate(RSocketRequest request) {
        String name = request.getString("name");
        String path = request.getString("path");
        
        if (name == null || name.isEmpty()) {
            return Mono.just(RSocketResponse.invalidParams("name is required"));
        }
        if (path == null || path.isEmpty()) {
            return Mono.just(RSocketResponse.invalidParams("path is required"));
        }
        
        if (sokybotContext == null) {
            return Mono.just(RSocketResponse.error(
                RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                "Sokybot context not available"
            ));
        }
        
        try {
            sokybotContext.installGroup(name, path);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "created");
            result.put("name", name);
            return Mono.just(RSocketResponse.success(result));
        } catch (Exception e) {
            logger.error("Failed to create group: {}", name, e);
            return Mono.just(RSocketResponse.internalError(e));
        }
    }
}
