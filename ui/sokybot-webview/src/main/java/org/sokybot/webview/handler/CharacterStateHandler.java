package org.sokybot.webview.handler;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;

import reactor.core.publisher.Mono;

/**
 * Handler for character state requests.
 * 
 * Method: character.state
 * Params:
 *   - machineId (optional): Machine identifier. If not provided, uses first available machine.
 */
@Component(
    service = IRSocketHandler.class,
    property = IRSocketHandler.METHOD_PROPERTY + "=character.state"
)
public class CharacterStateHandler implements IRSocketHandler {
    
    private volatile IGroupContext groupContext;
    
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setGroupContext(IGroupContext groupContext) {
        this.groupContext = groupContext;
    }
    
    protected void unsetGroupContext(IGroupContext groupContext) {
        this.groupContext = null;
    }
    
    @Override
    public String getMethod() {
        return "character.state";
    }
    
    @Override
    public String getDescription() {
        return "Get character state for a machine";
    }
    
    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        if (groupContext == null) {
            return Mono.just(RSocketResponse.error(
                RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                "Group context not available"
            ));
        }
        
        String machineId = request.getString("machineId");
        
        // If no machineId provided, use first available
        if (machineId == null || machineId.isEmpty()) {
            IMachineContext[] machines = groupContext.getMachines();
            if (machines.length > 0) {
                machineId = machines[0].getMachineName();
            } else {
                return Mono.just(RSocketResponse.notFound("No machines available"));
            }
        }
        
        // Extract simple name if full name provided
        String simpleName = machineId.contains(".")
                ? machineId.substring(machineId.lastIndexOf(".") + 1)
                : machineId;
        
        IMachineContext ctx = groupContext.findMachineCtx(simpleName).orElse(null);
        if (ctx == null) {
            return Mono.just(RSocketResponse.notFound("Machine not found: " + machineId));
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
