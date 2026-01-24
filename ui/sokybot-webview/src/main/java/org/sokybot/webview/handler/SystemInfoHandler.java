package org.sokybot.webview.handler;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.webview.RSocketHandlerRegistry;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Mono;

/**
 * Handler for system information requests.
 * 
 * Methods:
 *   - system.info: Get system information
 *   - system.methods: List available RSocket methods
 *   - system.streams: List available RSocket streams
 */
@Component(
    service = IRSocketHandler.class,
    property = {
        IRSocketHandler.METHOD_PROPERTY + "=system.info",
        IRSocketHandler.METHOD_PROPERTY + "=system.methods",
        IRSocketHandler.METHOD_PROPERTY + "=system.streams"
    }
)
public class SystemInfoHandler implements IRSocketHandler {
    
    private RSocketHandlerRegistry handlerRegistry;
    
    @Reference
    protected void setHandlerRegistry(RSocketHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }
    
    @Override
    public String[] getMethods() {
        return new String[] { "system.info", "system.methods", "system.streams" };
    }
    
    @Override
    public String getDescription() {
        return "System information (info, methods, streams)";
    }
    
    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        String method = request.getMethod();
        
        switch (method) {
            case "system.info":
                return handleInfo(request);
            case "system.methods":
                return handleMethods(request);
            case "system.streams":
                return handleStreams(request);
            default:
                return Mono.just(RSocketResponse.methodNotFound(method));
        }
    }
    
    private Mono<RSocketResponse> handleInfo(RSocketRequest request) {
        Map<String, Object> info = new HashMap<>();
        info.put("version", "1.0.0");
        info.put("protocol", "rsocket-json");
        info.put("protocolVersion", "1.0");
        
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("free", runtime.freeMemory());
        memory.put("total", runtime.totalMemory());
        memory.put("max", runtime.maxMemory());
        info.put("memory", memory);
        
        info.put("processors", runtime.availableProcessors());
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("osName", System.getProperty("os.name"));
        
        return Mono.just(RSocketResponse.success(info));
    }
    
    private Mono<RSocketResponse> handleMethods(RSocketRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("methods", handlerRegistry.getHandlerInfo());
        return Mono.just(RSocketResponse.success(result));
    }
    
    private Mono<RSocketResponse> handleStreams(RSocketRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("streams", handlerRegistry.getStreamHandlerInfo());
        return Mono.just(RSocketResponse.success(result));
    }
}
