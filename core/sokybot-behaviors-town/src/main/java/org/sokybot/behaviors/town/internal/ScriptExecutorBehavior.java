package org.sokybot.behaviors.town.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.town.internal.settings.TownSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.navigation.api.INavigator;
import org.sokybot.navigation.api.IPathfinder;
import org.sokybot.navigation.api.NavigationException;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.scripting.api.IScriptExecutionSnapshot;
import org.sokybot.scripting.api.IScriptModel;
import org.sokybot.scripting.api.ITravelScript;
import org.sokybot.scripting.api.ScriptCycleKeys;
import org.sokybot.scripting.api.ScriptPhase;
import org.sokybot.scripting.api.TravelCommand;
import org.sokybot.scripting.api.TravelCommandKind;
import org.sokybot.town.api.INpcInteractionFacade;
import org.sokybot.town.api.ITownPolicy;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.api.TownCycleKeys;
import org.sokybot.town.projections.api.ITownModel;

@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class ScriptExecutorBehavior implements IBehavior<TownSettings> {

    @Reference
    private ITownModel townModel;

    @Reference
    private IScriptModel scriptModel;

    @Reference
    private INavigator navigator;

    @Reference
    private INpcInteractionFacade npcFacade;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IPathfinder pathfinder;

    @Override
    public String id() {
        return ScriptCycleKeys.BEHAVIOR_EXECUTE_SCRIPT;
    }

    @Override
    public int order() {
        return 58;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return TownCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<TownSettings> settingsType() {
        return TownSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, TownSettings settings) {
        if (settings == null || !settings.isTownLoopEnabled()) {
            return false;
        }
        ITownPolicy policy = settings.toPolicy();
        if (!policy.isTravelScriptEnabled()) {
            return false;
        }
        String sid = policy.getTravelScriptId();
        if (sid == null || sid.isEmpty()) {
            return false;
        }
        if (!scriptModel.findById(sid).isPresent()) {
            return false;
        }
        Optional<ITownSnapshot> snap = townModel.snapshot(context.getMachineId());
        return snap.isPresent() && !snap.get().isDead();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, TownSettings settings) {
        ITownPolicy policy = settings.toPolicy();
        String scriptId = policy.getTravelScriptId();
        Optional<ITravelScript> scriptOpt = scriptModel.findById(scriptId);
        if (!scriptOpt.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        ITravelScript script = scriptOpt.get();

        scriptModel.bindActiveScript(context.getMachineId(), scriptId, policy.getArrivalToleranceWorldUnits());
        context.getPersistentData().put(ScriptCycleKeys.KEY_ACTIVE_SCRIPT_ID, scriptId);

        Optional<IScriptExecutionSnapshot> snapOpt = scriptModel.snapshot(context.getMachineId());
        if (!snapOpt.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        IScriptExecutionSnapshot snap = snapOpt.get();

        if (snap.getPhase() == ScriptPhase.ERROR) {
            return BehaviorStatus.SKIPPED;
        }
        if (snap.getPhase() != ScriptPhase.IDLE) {
            return BehaviorStatus.SKIPPED;
        }

        int cursor = snap.getCursor();
        if (cursor >= script.size()) {
            scriptModel.reset(context.getMachineId());
            context.getPersistentData().remove(ScriptCycleKeys.KEY_ACTIVE_SCRIPT_ID);
            return BehaviorStatus.EXECUTED;
        }

        TravelCommand cmd = script.getCommands().get(cursor);
        TravelCommandKind kind = cmd.getKind();

        switch (kind) {
            case WALK:
                return executeWalk(context, cmd);
            case PORTAL:
                return executePortal(context, cmd);
            case TELEPORT:
                return executeTeleport(context, cmd);
            case WAIT:
                scriptModel.onWaitScheduled(context.getMachineId(), cmd.getWaitMs());
                touchStepTimestamp(context);
                return BehaviorStatus.EXECUTED;
            case LOG:
                context.log("INFO", cmd.getMessage() != null ? cmd.getMessage() : "");
                scriptModel.onLogLineExecuted(context.getMachineId());
                touchStepTimestamp(context);
                return BehaviorStatus.EXECUTED;
            case WAIT_FOR_LOAD:
            default:
                return BehaviorStatus.SKIPPED;
        }
    }

    private BehaviorStatus executeWalk(IWorkflowContext context, TravelCommand cmd) {
        WorldPoint target = cmd.getPoint();
        if (target == null) {
            return BehaviorStatus.SKIPPED;
        }
        IPathfinder pf = pathfinder;
        if (pf != null && context.getGameModel() != null && context.getGameModel().getTrainer() != null) {
            GamePosition pos = context.getGameModel().getTrainer().getPosition();
            if (pos != null) {
                WorldPoint origin = new WorldPoint(pos.getX(), pos.getY(), pos.getZ());
                if (!pf.isReachable(origin, target)) {
                    context.log("WARN", "Travel script walk target not reachable by pathfinder");
                    return BehaviorStatus.SKIPPED;
                }
            }
        }
        scriptModel.onWalkProbeBound(context.getMachineId(), target, false);
        try {
            navigator.walkTo(context, target);
        } catch (NavigationException ex) {
            context.log("WARN", "Travel script walk failed: {}", ex.getMessage());
            return BehaviorStatus.SKIPPED;
        }
        touchStepTimestamp(context);
        return BehaviorStatus.EXECUTED;
    }

    private BehaviorStatus executePortal(IWorkflowContext context, TravelCommand cmd) {
        WorldPoint target = cmd.getPoint();
        if (target == null) {
            return BehaviorStatus.SKIPPED;
        }
        IPathfinder pf = pathfinder;
        if (pf != null && context.getGameModel() != null && context.getGameModel().getTrainer() != null) {
            GamePosition pos = context.getGameModel().getTrainer().getPosition();
            if (pos != null) {
                WorldPoint origin = new WorldPoint(pos.getX(), pos.getY(), pos.getZ());
                if (!pf.isReachable(origin, target)) {
                    context.log("WARN", "Travel script portal target not reachable by pathfinder");
                    return BehaviorStatus.SKIPPED;
                }
            }
        }
        scriptModel.onWalkProbeBound(context.getMachineId(), target, true);
        try {
            navigator.walkTo(context, target);
        } catch (NavigationException ex) {
            context.log("WARN", "Travel script portal walk failed: {}", ex.getMessage());
            return BehaviorStatus.SKIPPED;
        }
        touchStepTimestamp(context);
        return BehaviorStatus.EXECUTED;
    }

    private BehaviorStatus executeTeleport(IWorkflowContext context, TravelCommand cmd) {
        scriptModel.onTeleportStepIssued(context.getMachineId());
        npcFacade.teleport(context, cmd.getNpcRefId(), cmd.getDestinationRefId())
                .whenComplete((res, err) -> {
                    if (err != null) {
                        context.log("WARN", "Travel script teleport failed: {}", err.toString());
                    } else if (res != null && !res.isSuccess()) {
                        context.log("WARN", "Travel script teleport rejected: {}",
                                res.getDetailMessage().orElse("unknown"));
                    }
                });
        touchStepTimestamp(context);
        return BehaviorStatus.EXECUTED;
    }

    private static void touchStepTimestamp(IWorkflowContext context) {
        context.getPersistentData().put(ScriptCycleKeys.KEY_LAST_STEP_AT_MS, System.currentTimeMillis());
    }
}
