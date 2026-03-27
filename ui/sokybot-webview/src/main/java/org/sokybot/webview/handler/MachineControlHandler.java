package org.sokybot.webview.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Mono;

/**
 * Handler for machine control operations.
 * 
 * Methods:
 * - machine.start: Start a bot machine
 * - machine.stop: Stop a bot machine
 * - machine.list: List all machines
 * - machine.create: Create a new machine
 */
@Component(service = IRSocketHandler.class, property = {
        IRSocketHandler.METHOD_PROPERTY + "=machine.start",
        IRSocketHandler.METHOD_PROPERTY + "=machine.stop",
        IRSocketHandler.METHOD_PROPERTY + "=machine.list",
        IRSocketHandler.METHOD_PROPERTY + "=machine.create",
        IRSocketHandler.METHOD_PROPERTY + "=machine.initialize"
})
public class MachineControlHandler implements IRSocketHandler {

    private volatile IGroupContext groupContext;
    private volatile ISokybotContext sokybotContext;
    private volatile org.sokybot.settings.api.ISettingsRegistry settingsRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setGroupContext(IGroupContext groupContext) {
        this.groupContext = groupContext;
    }

    protected void unsetGroupContext(IGroupContext groupContext) {
        this.groupContext = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setSokybotContext(ISokybotContext sokybotContext) {
        this.sokybotContext = sokybotContext;
    }

    protected void unsetSokybotContext(ISokybotContext sokybotContext) {
        this.sokybotContext = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setSettingsRegistry(org.sokybot.settings.api.ISettingsRegistry settingsRegistry) {
        this.settingsRegistry = settingsRegistry;
    }

    protected void unsetSettingsRegistry(org.sokybot.settings.api.ISettingsRegistry settingsRegistry) {
        this.settingsRegistry = null;
    }

    @Override
    public String[] getMethods() {
        return new String[] { "machine.start", "machine.stop", "machine.list", "machine.create", "machine.initialize" };
    }

    @Override
    public String getDescription() {
        return "Machine control operations (start, stop, list, create, initialize)";
    }

    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        String method = request.getMethod();

        switch (method) {
            case "machine.start":
                return handleStart(request);
            case "machine.stop":
                return handleStop(request);
            case "machine.list":
                return handleList(request);
            case "machine.create":
                return handleCreate(request);
            case "machine.initialize":
                return handleInitialize(request);
            default:
                return Mono.just(RSocketResponse.methodNotFound(method));
        }
    }

    private Mono<RSocketResponse> handleStart(RSocketRequest request) {
        String machineId = request.getString("machineId");

        if (machineId == null || machineId.isEmpty()) {
            return Mono.just(RSocketResponse.invalidParams("machineId is required"));
        }

        IMachineContext ctx = findMachine(machineId).orElse(null);
        if (ctx == null) {
            return Mono.just(RSocketResponse.notFound("Machine not found: " + machineId));
        }

        try {
            boolean alreadyRunning = ctx.isRunning();
            if (!alreadyRunning) {
                ctx.getEngine().start();
            }
            ctx.getEngine().sendEvent("CONNECT");
            Map<String, Object> result = new HashMap<>();
            result.put("status", alreadyRunning ? "already_running" : "started");
            result.put("machineId", machineId);
            return Mono.just(RSocketResponse.success(result));
        } catch (Exception e) {
            return Mono.just(RSocketResponse.internalError(e));
        }
    }

    private Mono<RSocketResponse> handleStop(RSocketRequest request) {
        String machineId = request.getString("machineId");

        if (machineId == null || machineId.isEmpty()) {
            return Mono.just(RSocketResponse.invalidParams("machineId is required"));
        }

        IMachineContext ctx = findMachine(machineId).orElse(null);
        if (ctx == null) {
            return Mono.just(RSocketResponse.notFound("Machine not found: " + machineId));
        }

        try {
            ctx.getEngine().stop();
            Map<String, Object> result = new HashMap<>();
            result.put("status", "stopped");
            result.put("machineId", machineId);
            return Mono.just(RSocketResponse.success(result));
        } catch (Exception e) {
            return Mono.just(RSocketResponse.internalError(e));
        }
    }

    private java.util.Optional<IMachineContext> findMachine(String machineId) {
        if (sokybotContext != null) {
            // Try to look up by Group.Machine format
            int dotIndex = machineId.indexOf('.');
            if (dotIndex > 0) {
                String groupName = machineId.substring(0, dotIndex);
                String machineName = machineId.substring(dotIndex + 1);

                java.util.Optional<IGroupContext> group = sokybotContext.findGroupCtx(groupName);
                if (group.isPresent()) {
                    java.util.Optional<IMachineContext> machine = group.get().findMachineCtx(machineName);
                    if (machine.isPresent()) {
                        return machine;
                    }
                }
            }

            // Fallback: Search all groups (e.g. if machineId is just the name)
            for (IGroupContext group : sokybotContext.getGroups()) {
                java.util.Optional<IMachineContext> machine = group.findMachineCtx(machineId);
                if (machine.isPresent()) {
                    return machine;
                }
            }
        }

        if (groupContext != null) {
            return groupContext.findMachineCtx(machineId);
        }
        return java.util.Optional.empty();
    }

    private Mono<RSocketResponse> handleList(RSocketRequest request) {
        Map<String, Map<String, Object>> machineMap = new java.util.LinkedHashMap<>();

        if (sokybotContext != null) {
            for (IGroupContext group : sokybotContext.getGroups()) {
                for (IMachineContext machine : group.getMachines()) {
                    Map<String, Object> info = new HashMap<>();
                    info.put("machineId", machine.fullName());
                    info.put("name", machine.getMachineName());
                    info.put("groupName", group.name());
                    info.put("isRunning", machine.isRunning());
                    machineMap.put(machine.fullName(), info);
                }
            }
        } else if (groupContext != null) {
            for (IMachineContext machine : groupContext.getMachines()) {
                Map<String, Object> info = new HashMap<>();
                info.put("machineId", machine.fullName());
                info.put("name", machine.getMachineName());
                info.put("isRunning", machine.isRunning());
                machineMap.put(machine.fullName(), info);
            }
        }

        List<Map<String, Object>> machines = new ArrayList<>(machineMap.values());
        return Mono.just(RSocketResponse.success(machines));
    }

    private Mono<RSocketResponse> handleCreate(RSocketRequest request) {
        String group = request.getString("group");
        String name = request.getString("name");

        if (group == null || group.isEmpty()) {
            return Mono.just(RSocketResponse.invalidParams("group is required"));
        }
        if (name == null || name.isEmpty()) {
            return Mono.just(RSocketResponse.invalidParams("name is required"));
        }

        if (sokybotContext == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Sokybot context not available"));
        }

        IGroupContext grpCtx = sokybotContext.findGroupCtx(group).orElse(null);
        if (grpCtx == null) {
            return Mono.just(RSocketResponse.notFound("Group not found: " + group));
        }

        try {
            grpCtx.installMachine(name);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "created");
            result.put("machineId", group + "." + name);
            return Mono.just(RSocketResponse.success(result));
        } catch (Exception e) {
            return Mono.just(RSocketResponse.internalError(e));
        }
    }

    @SuppressWarnings("unchecked")
    private Mono<RSocketResponse> handleInitialize(RSocketRequest request) {
        String group = request.getString("group");
        String name = request.getString("name");
        String scope = request.getString("scope");
        Map<String, Object> payload = request.get("payload", Map.class);

        if (group == null || name == null || scope == null || payload == null) {
            return Mono.just(RSocketResponse.invalidParams("group, name, scope, and payload are required"));
        }

        if (settingsRegistry == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Settings registry not available"));
        }

        try {
            settingsRegistry.writeRawSettings(group, name, scope, payload);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "initialized");
            result.put("machineId", group + "." + name);
            return Mono.just(RSocketResponse.success(result));
        } catch (Exception e) {
            return Mono.just(RSocketResponse.internalError(e));
        }
    }
}
