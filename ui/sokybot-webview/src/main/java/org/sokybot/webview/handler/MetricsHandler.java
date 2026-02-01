package org.sokybot.webview.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.commons.metrics.Counter;
import org.sokybot.commons.metrics.Gauge;
import org.sokybot.commons.metrics.IMetric;
import org.sokybot.commons.metrics.IMetricsRegistry;
import org.sokybot.commons.metrics.Timer;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Mono;

/**
 * RSocket handler for metrics operations.
 * 
 * Methods:
 * - metrics.list: List all metrics
 * - metrics.get: Get a specific metric
 * - metrics.reset: Reset all or specific metrics
 */
@Component(service = IRSocketHandler.class, property = {
        IRSocketHandler.METHOD_PROPERTY + "=metrics.list",
        IRSocketHandler.METHOD_PROPERTY + "=metrics.get",
        IRSocketHandler.METHOD_PROPERTY + "=metrics.reset"
})
public class MetricsHandler implements IRSocketHandler {

    private volatile IMetricsRegistry metricsRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setMetricsRegistry(IMetricsRegistry registry) {
        this.metricsRegistry = registry;
    }

    protected void unsetMetricsRegistry(IMetricsRegistry registry) {
        this.metricsRegistry = null;
    }

    @Override
    public String[] getMethods() {
        return new String[] { "metrics.list", "metrics.get", "metrics.reset" };
    }

    @Override
    public String getDescription() {
        return "Metrics and telemetry operations";
    }

    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        String method = request.getMethod();

        switch (method) {
            case "metrics.list":
                return handleList(request);
            case "metrics.get":
                return handleGet(request);
            case "metrics.reset":
                return handleReset(request);
            default:
                return Mono.just(RSocketResponse.methodNotFound(method));
        }
    }

    private Mono<RSocketResponse> handleList(RSocketRequest request) {
        if (metricsRegistry == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Metrics registry not available"));
        }

        String prefix = request.getString("prefix");
        Map<String, IMetric> metrics = prefix != null && !prefix.isEmpty()
                ? metricsRegistry.getByPrefix(prefix)
                : metricsRegistry.getAll();

        List<Map<String, Object>> metricList = new ArrayList<>();
        for (Map.Entry<String, IMetric> entry : metrics.entrySet()) {
            metricList.add(metricToMap(entry.getValue(), false));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("metrics", metricList);
        response.put("count", metricList.size());

        return Mono.just(RSocketResponse.success(response));
    }

    private Mono<RSocketResponse> handleGet(RSocketRequest request) {
        if (metricsRegistry == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Metrics registry not available"));
        }

        String name = request.getString("name");
        if (name == null || name.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: name"));
        }

        return metricsRegistry.get(name)
                .map(metric -> RSocketResponse.success(metricToMap(metric, true)))
                .map(Mono::just)
                .orElse(Mono.just(RSocketResponse.error(
                        RSocketResponse.ErrorCode.NOT_FOUND,
                        "Metric not found: " + name)));
    }

    private Mono<RSocketResponse> handleReset(RSocketRequest request) {
        if (metricsRegistry == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Metrics registry not available"));
        }

        String name = request.getString("name");

        if (name != null && !name.isEmpty()) {
            // Reset specific metric
            return metricsRegistry.get(name)
                    .map(metric -> {
                        metric.reset();
                        Map<String, Object> response = new HashMap<>();
                        response.put("name", name);
                        response.put("reset", true);
                        return RSocketResponse.success(response);
                    })
                    .map(Mono::just)
                    .orElse(Mono.just(RSocketResponse.error(
                            RSocketResponse.ErrorCode.NOT_FOUND,
                            "Metric not found: " + name)));
        } else {
            // Reset all metrics
            int count = metricsRegistry.size();
            metricsRegistry.resetAll();

            Map<String, Object> response = new HashMap<>();
            response.put("resetCount", count);
            response.put("reset", true);

            return Mono.just(RSocketResponse.success(response));
        }
    }

    private Map<String, Object> metricToMap(IMetric metric, boolean includeDetails) {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("name", metric.getName());
        map.put("type", metric.getType().name().toLowerCase());
        map.put("value", metric.getValue());

        if (includeDetails) {
            map.put("description", metric.getDescription());

            // Add type-specific details
            if (metric instanceof Counter) {
                Counter counter = (Counter) metric;
                map.put("count", counter.getCount());
            } else if (metric instanceof Gauge) {
                Gauge gauge = (Gauge) metric;
                map.put("currentValue", gauge.get());
            } else if (metric instanceof Timer) {
                Timer timer = (Timer) metric;
                map.put("count", timer.getCount());
                map.put("totalMs", timer.getTotalMillis());
                map.put("avgMs", timer.getAverageMillis());
                map.put("minMs", timer.getMinMillis());
                map.put("maxMs", timer.getMaxMillis());
            }
        }

        return map;
    }
}
