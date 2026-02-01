package org.sokybot.webview.handler;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.commons.health.HealthCheckResult;
import org.sokybot.commons.health.IHealthCheckRegistry;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Mono;

/**
 * RSocket handler for health check operations.
 * 
 * Methods:
 * - system.health: Get overall system health and component statuses
 * - system.health.check: Run a specific health check by name
 * - system.health.list: List available health checks
 */
@Component(service = IRSocketHandler.class, property = {
        IRSocketHandler.METHOD_PROPERTY + "=system.health",
        IRSocketHandler.METHOD_PROPERTY + "=system.health.check",
        IRSocketHandler.METHOD_PROPERTY + "=system.health.list"
})
public class HealthHandler implements IRSocketHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private volatile IHealthCheckRegistry healthCheckRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setHealthCheckRegistry(IHealthCheckRegistry registry) {
        this.healthCheckRegistry = registry;
    }

    protected void unsetHealthCheckRegistry(IHealthCheckRegistry registry) {
        this.healthCheckRegistry = null;
    }

    @Override
    public String[] getMethods() {
        return new String[] { "system.health", "system.health.check", "system.health.list" };
    }

    @Override
    public String getDescription() {
        return "System health check operations";
    }

    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        String method = request.getMethod();

        switch (method) {
            case "system.health":
                return handleHealth(request);
            case "system.health.check":
                return handleCheck(request);
            case "system.health.list":
                return handleList(request);
            default:
                return Mono.just(RSocketResponse.methodNotFound(method));
        }
    }

    private Mono<RSocketResponse> handleHealth(RSocketRequest request) {
        if (healthCheckRegistry == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Health check registry not available"));
        }

        Map<String, HealthCheckResult> results = healthCheckRegistry.checkAll();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", healthCheckRegistry.getOverallStatus().name());

        Map<String, Object> components = new LinkedHashMap<>();
        for (Map.Entry<String, HealthCheckResult> entry : results.entrySet()) {
            components.put(entry.getKey(), resultToMap(entry.getValue()));
        }
        response.put("components", components);

        return Mono.just(RSocketResponse.success(response));
    }

    private Mono<RSocketResponse> handleCheck(RSocketRequest request) {
        if (healthCheckRegistry == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Health check registry not available"));
        }

        String name = request.getString("name");
        if (name == null || name.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: name"));
        }

        return healthCheckRegistry.check(name)
                .map(result -> RSocketResponse.success(resultToMap(result)))
                .map(Mono::just)
                .orElse(Mono.just(RSocketResponse.error(
                        RSocketResponse.ErrorCode.NOT_FOUND,
                        "Health check not found: " + name)));
    }

    private Mono<RSocketResponse> handleList(RSocketRequest request) {
        if (healthCheckRegistry == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Health check registry not available"));
        }

        List<String> names = healthCheckRegistry.getHealthCheckNames();

        Map<String, Object> response = new HashMap<>();
        response.put("checks", names);
        response.put("count", names.size());

        return Mono.just(RSocketResponse.success(response));
    }

    private Map<String, Object> resultToMap(HealthCheckResult result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", result.getStatus().name());
        map.put("category", result.getCategory().name());
        map.put("timestamp", TIMESTAMP_FORMATTER.format(result.getTimestamp()));

        if (result.getMessage() != null) {
            map.put("message", result.getMessage());
        }

        return map;
    }
}
