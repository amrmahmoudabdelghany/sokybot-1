package org.sokybot.webview.handler;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.engine.api.extension.ActuatorDescriptor;
import org.sokybot.engine.plugin.IActuatorLoader;
import org.sokybot.engine.plugin.LoadedActuator;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Mono;

/**
 * RSocket handler for actuator management.
 * 
 * Methods:
 * - actuator.list: List all loaded actuators
 * - actuator.info: Get details about a specific actuator
 * - actuator.enable: Enable an actuator
 * - actuator.disable: Disable an actuator
 * - actuator.reload: Reload actuators from plugin directory
 */
@Component(service = IRSocketHandler.class, property = {
        IRSocketHandler.METHOD_PROPERTY + "=actuator.list",
        IRSocketHandler.METHOD_PROPERTY + "=actuator.info",
        IRSocketHandler.METHOD_PROPERTY + "=actuator.enable",
        IRSocketHandler.METHOD_PROPERTY + "=actuator.disable",
        IRSocketHandler.METHOD_PROPERTY + "=actuator.reload"
})
public class ActuatorHandler implements IRSocketHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private volatile IActuatorLoader actuatorLoader;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setActuatorLoader(IActuatorLoader loader) {
        this.actuatorLoader = loader;
    }

    protected void unsetActuatorLoader(IActuatorLoader loader) {
        this.actuatorLoader = null;
    }

    @Override
    public String[] getMethods() {
        return new String[] {
                "actuator.list",
                "actuator.info",
                "actuator.enable",
                "actuator.disable",
                "actuator.reload"
        };
    }

    @Override
    public String getDescription() {
        return "Actuator plugin management";
    }

    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        String method = request.getMethod();

        switch (method) {
            case "actuator.list":
                return handleList(request);
            case "actuator.info":
                return handleInfo(request);
            case "actuator.enable":
                return handleEnable(request);
            case "actuator.disable":
                return handleDisable(request);
            case "actuator.reload":
                return handleReload(request);
            default:
                return Mono.just(RSocketResponse.methodNotFound(method));
        }
    }

    private Mono<RSocketResponse> handleList(RSocketRequest request) {
        if (actuatorLoader == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Actuator loader not available"));
        }

        List<LoadedActuator> actuators = actuatorLoader.getLoadedActuators();

        List<Map<String, Object>> actuatorList = new ArrayList<>();
        for (LoadedActuator loaded : actuators) {
            actuatorList.add(actuatorToMap(loaded, false));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("actuators", actuatorList);
        response.put("count", actuatorList.size());
        response.put("pluginDirectory", actuatorLoader.getPluginDirectory().toString());

        return Mono.just(RSocketResponse.success(response));
    }

    private Mono<RSocketResponse> handleInfo(RSocketRequest request) {
        if (actuatorLoader == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Actuator loader not available"));
        }

        String name = request.getString("name");
        if (name == null || name.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: name"));
        }

        return actuatorLoader.getActuator(name)
                .map(loaded -> RSocketResponse.success(actuatorToMap(loaded, true)))
                .map(Mono::just)
                .orElse(Mono.just(RSocketResponse.error(
                        RSocketResponse.ErrorCode.NOT_FOUND,
                        "Actuator not found: " + name)));
    }

    private Mono<RSocketResponse> handleEnable(RSocketRequest request) {
        if (actuatorLoader == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Actuator loader not available"));
        }

        String name = request.getString("name");
        if (name == null || name.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: name"));
        }

        return actuatorLoader.getActuator(name)
                .map(loaded -> {
                    loaded.setEnabled(true);
                    Map<String, Object> response = new HashMap<>();
                    response.put("name", name);
                    response.put("enabled", true);
                    return RSocketResponse.success(response);
                })
                .map(Mono::just)
                .orElse(Mono.just(RSocketResponse.error(
                        RSocketResponse.ErrorCode.NOT_FOUND,
                        "Actuator not found: " + name)));
    }

    private Mono<RSocketResponse> handleDisable(RSocketRequest request) {
        if (actuatorLoader == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Actuator loader not available"));
        }

        String name = request.getString("name");
        if (name == null || name.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: name"));
        }

        return actuatorLoader.getActuator(name)
                .map(loaded -> {
                    loaded.setEnabled(false);
                    Map<String, Object> response = new HashMap<>();
                    response.put("name", name);
                    response.put("enabled", false);
                    return RSocketResponse.success(response);
                })
                .map(Mono::just)
                .orElse(Mono.just(RSocketResponse.error(
                        RSocketResponse.ErrorCode.NOT_FOUND,
                        "Actuator not found: " + name)));
    }

    private Mono<RSocketResponse> handleReload(RSocketRequest request) {
        if (actuatorLoader == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Actuator loader not available"));
        }

        int count = actuatorLoader.reloadAll();

        Map<String, Object> response = new HashMap<>();
        response.put("reloaded", count);
        response.put("pluginDirectory", actuatorLoader.getPluginDirectory().toString());

        return Mono.just(RSocketResponse.success(response));
    }

    private Map<String, Object> actuatorToMap(LoadedActuator loaded, boolean includeDetails) {
        Map<String, Object> map = new LinkedHashMap<>();
        ActuatorDescriptor desc = loaded.getDescriptor();

        map.put("name", loaded.getName());
        map.put("displayName", desc.getDisplayName());
        map.put("version", desc.getVersion());
        map.put("enabled", loaded.isEnabled());
        map.put("source", loaded.getLoadSource().name());

        if (includeDetails) {
            map.put("description", desc.getDescription());
            map.put("author", desc.getAuthor());
            map.put("dependencies", desc.getDependencies());
            map.put("tags", desc.getTags());
            map.put("loadedAt", TIMESTAMP_FORMATTER.format(loaded.getLoadedAt()));
            if (loaded.getSourcePath() != null) {
                map.put("sourcePath", loaded.getSourcePath().toString());
            }
        }

        return map;
    }
}
