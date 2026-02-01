package org.sokybot.webview.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;
import org.sokybot.webview.api.doc.RSocketMethod;
import org.sokybot.webview.api.doc.RSocketMethods;
import org.sokybot.webview.api.doc.RSocketParam;

import reactor.core.publisher.Mono;

/**
 * RSocket handler for API documentation.
 * Scans all registered IRSocketHandlers for @RSocketMethod annotations.
 */
@Component(service = IRSocketHandler.class, property = {
        IRSocketHandler.METHOD_PROPERTY + "=api.docs"
})
public class ApiDocHandler implements IRSocketHandler {

    private final List<IRSocketHandler> handlers = new ArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindHandler(IRSocketHandler handler) {
        synchronized (handlers) {
            handlers.add(handler);
        }
    }

    protected void unbindHandler(IRSocketHandler handler) {
        synchronized (handlers) {
            handlers.remove(handler);
        }
    }

    @Override
    public String[] getMethods() {
        return new String[] { "api.docs" };
    }

    @Override
    public String getDescription() {
        return "API Documentation Generator";
    }

    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        return Mono.just(RSocketResponse.success(generateDocs()));
    }

    private Map<String, Object> generateDocs() {
        Map<String, Object> docs = new TreeMap<>();

        List<IRSocketHandler> currentHandlers;
        synchronized (handlers) {
            currentHandlers = new ArrayList<>(handlers);
        }

        for (IRSocketHandler handler : currentHandlers) {
            processHandler(handler, docs);
        }

        return docs;
    }

    private void processHandler(IRSocketHandler handler, Map<String, Object> docs) {
        Class<?> clazz = handler.getClass();

        // Process repeatable annotations
        RSocketMethod[] methods = clazz.getAnnotationsByType(RSocketMethod.class);

        for (RSocketMethod method : methods) {
            Map<String, Object> methodDoc = new HashMap<>();
            methodDoc.put("description", method.description());
            methodDoc.put("returnType", method.returnType());

            List<Map<String, Object>> params = new ArrayList<>();
            for (RSocketParam param : method.params()) {
                Map<String, Object> paramDoc = new HashMap<>();
                paramDoc.put("name", param.name());
                paramDoc.put("type", param.type());
                paramDoc.put("required", param.required());
                paramDoc.put("description", param.description());
                params.add(paramDoc);
            }
            methodDoc.put("params", params);

            docs.put(method.name(), methodDoc);
        }

        // Fallback for methods without annotation
        for (String methodName : handler.getMethods()) {
            if (!docs.containsKey(methodName)) {
                Map<String, Object> methodDoc = new HashMap<>();
                methodDoc.put("description",
                        handler.getDescription() != null ? handler.getDescription() : "Undocumented");
                methodDoc.put("undocumented", true);
                docs.put(methodName, methodDoc);
            }
        }
    }
}
