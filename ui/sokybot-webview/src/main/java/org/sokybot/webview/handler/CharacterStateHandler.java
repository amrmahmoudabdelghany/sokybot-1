package org.sokybot.webview.handler;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Mono;

/**
 * Handler for character state requests.
 * 
 * Method: character.state
 * Params:
 * - machineId (optional): Machine identifier. If not provided, uses first
 * available machine.
 */
@Component(service = IRSocketHandler.class, property = IRSocketHandler.METHOD_PROPERTY + "=character.state")
public class CharacterStateHandler implements IRSocketHandler {

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
        return new String[] { "character.state" };
    }

    @Override
    public String getDescription() {
        return "Get character state for a machine";
    }

    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        if (sokybotContext == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Sokybot context not available"));
        }

        String machineId = request.getString("machineId");
        IMachineContext ctx = null;

        // If no machineId provided, use first available from first group
        if (machineId == null || machineId.isEmpty()) {
            for (IGroupContext group : sokybotContext.getGroups()) {
                IMachineContext[] machines = group.getMachines();
                if (machines.length > 0) {
                    ctx = machines[0];
                    break;
                }
            }
            if (ctx == null) {
                return Mono.just(RSocketResponse.notFound("No machines available"));
            }
        } else {
            // Try to find machine by full name (Group.Machine) or simple name
            String groupName = null;
            String machineName = machineId;

            if (machineId.contains(".")) {
                String[] parts = machineId.split("\\.", 2);
                groupName = parts[0];
                machineName = parts[1];
            }

            if (groupName != null) {
                // Lookup by specific group
                IGroupContext group = sokybotContext.findGroupCtx(groupName).orElse(null);
                if (group != null) {
                    ctx = group.findMachineCtx(machineName).orElse(null);
                }
            } else {
                // Scan all groups for machine name
                for (IGroupContext group : sokybotContext.getGroups()) {
                    ctx = group.findMachineCtx(machineName).orElse(null);
                    if (ctx != null)
                        break;
                }
            }

            if (ctx == null) {
                return Mono.just(RSocketResponse.notFound("Machine not found: " + machineId));
            }
        }

        ITrainer trainer = null;
        if (ctx.getGameModel() != null) {
            trainer = ctx.getGameModel().getTrainer();
        }

        if (trainer == null) {
            return Mono.just(RSocketResponse.success(new HashMap<>()));
        }

        Map<String, Object> state = new HashMap<>();
        state.put("entityId", trainer.getUniqueId());
        state.put("characterName", trainer.getName());
        state.put("level", trainer.getLevel());
        state.put("currentHP", trainer.getCurrentHP());
        state.put("maxHP", trainer.getMaxHP());
        state.put("currentMP", trainer.getCurrentMP());
        state.put("maxMP", trainer.getMaxMP());
        state.put("gold", trainer.getGold());
        state.put("xSector", trainer.getRefId());
        state.put("x", trainer.getX());
        state.put("y", trainer.getY());
        state.put("isRunning", ctx.isRunning());

        return Mono.just(RSocketResponse.success(state));
    }
}
