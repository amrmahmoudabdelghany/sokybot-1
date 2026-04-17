package org.sokybot.webview.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.sokybot.engine.api.event.Connect;
import org.sokybot.engine.api.event.Wake;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.runtime.RuntimeEntityNames;
import org.sokybot.webview.api.dto.MachineActionResultDto;
import org.sokybot.webview.api.dto.MachineInfoDto;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.util.MachineResolver;
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

        ISokybotContext sokybotCtx = this.sokybotContext;
        IGroupContext groupCtx = this.groupContext;
        IMachineContext ctx = findMachine(machineId, sokybotCtx, groupCtx).orElse(null);
        if (ctx == null) {
            return Mono.just(RSocketResponse.notFound("Machine not found: " + machineId));
        }

        try {
            boolean alreadyRunning = ctx.isRunning();
            List<String> activities = alreadyRunning
                    ? ctx.getEngine().getActiveActivities()
                    : Collections.emptyList();
            boolean alreadyConnecting = activities.contains("LOGIN");
            if (alreadyConnecting) {
                return Mono.just(RSocketResponse.success(new MachineActionResultDto("already_connecting", machineId)));
            }
            if (alreadyRunning) {
                ctx.getEngine().dispatch(Wake.INSTANCE);
                return Mono.just(RSocketResponse.success(new MachineActionResultDto("already_running", machineId)));
            }
            ctx.getEngine().start();
            ctx.getEngine().dispatch(Connect.INSTANCE);
            ctx.getEngine().dispatch(Wake.INSTANCE);
            return Mono.just(RSocketResponse.success(new MachineActionResultDto("started", machineId)));
        } catch (Exception e) {
            return Mono.just(RSocketResponse.internalError(e));
        }
    }

    private Mono<RSocketResponse> handleStop(RSocketRequest request) {
        String machineId = request.getString("machineId");

        if (machineId == null || machineId.isEmpty()) {
            return Mono.just(RSocketResponse.invalidParams("machineId is required"));
        }

        ISokybotContext sokybotCtx = this.sokybotContext;
        IGroupContext groupCtx = this.groupContext;
        IMachineContext ctx = findMachine(machineId, sokybotCtx, groupCtx).orElse(null);
        if (ctx == null) {
            return Mono.just(RSocketResponse.notFound("Machine not found: " + machineId));
        }

        try {
            ctx.getEngine().stop();
            return Mono.just(RSocketResponse.success(new MachineActionResultDto("stopped", machineId)));
        } catch (Exception e) {
            return Mono.just(RSocketResponse.internalError(e));
        }
    }

    private java.util.Optional<IMachineContext> findMachine(String machineId, ISokybotContext sokybotCtx, IGroupContext groupCtx) {
        return MachineResolver.resolve(sokybotCtx, groupCtx, machineId);
    }

    private Mono<RSocketResponse> handleList(RSocketRequest request) {
        ISokybotContext sokybotCtx = this.sokybotContext;
        IGroupContext groupCtx = this.groupContext;
        Map<String, MachineInfoDto> machineMap = new java.util.LinkedHashMap<>();

        if (sokybotCtx != null) {
            for (IGroupContext group : sokybotCtx.getGroups()) {
                for (IMachineContext machine : group.getMachines()) {
                    MachineInfoDto info = new MachineInfoDto(machine.fullName(), machine.getMachineName(), group.name(), machine.isRunning());
                    machineMap.put(machine.fullName(), info);
                }
            }
        } else if (groupCtx != null) {
            for (IMachineContext machine : groupCtx.getMachines()) {
                MachineInfoDto info = new MachineInfoDto(machine.fullName(), machine.getMachineName(), groupCtx.name(), machine.isRunning());
                machineMap.put(machine.fullName(), info);
            }
        }

        List<MachineInfoDto> machines = new ArrayList<>(machineMap.values());
        return Mono.just(RSocketResponse.success(machines));
    }

    private Mono<RSocketResponse> handleCreate(RSocketRequest request) {
        ISokybotContext sokybotCtx = this.sokybotContext;
        String group = request.getString("group");
        String name = request.getString("name");

        if (group == null || group.isEmpty()) {
            return Mono.just(RSocketResponse.invalidParams("group is required"));
        }
        if (name == null || name.isEmpty()) {
            return Mono.just(RSocketResponse.invalidParams("name is required"));
        }

        if (sokybotCtx == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Sokybot context not available"));
        }

        IGroupContext grpCtx = sokybotCtx.findGroupCtx(group).orElse(null);
        if (grpCtx == null) {
            return Mono.just(RSocketResponse.notFound("Group not found: " + group));
        }

        try {
            RuntimeEntityNames.validateMachineOrThrow(name);
            grpCtx.installMachine(name);

            return Mono.just(RSocketResponse.success(new MachineActionResultDto("created", group + "." + name)));
        } catch (IllegalArgumentException e) {
            return Mono.just(RSocketResponse.invalidParams(e.getMessage()));
        } catch (Exception e) {
            return Mono.just(RSocketResponse.internalError(e));
        }
    }

    @SuppressWarnings("unchecked")
    private Mono<RSocketResponse> handleInitialize(RSocketRequest request) {
        org.sokybot.settings.api.ISettingsRegistry localSettingsRegistry = this.settingsRegistry;
        String group = request.getString("group");
        String name = request.getString("name");
        String scope = request.getString("scope");
        Map<String, Object> payload = request.get("payload", Map.class);

        if (group == null || name == null || scope == null || payload == null) {
            return Mono.just(RSocketResponse.invalidParams("group, name, scope, and payload are required"));
        }

        if (localSettingsRegistry == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Settings registry not available"));
        }

        try {
            localSettingsRegistry.writeRawSettings(group, name, scope, payload);

            return Mono.just(RSocketResponse.success(new MachineActionResultDto("initialized", group + "." + name)));
        } catch (Exception e) {
            return Mono.just(RSocketResponse.internalError(e));
        }
    }
}
