package org.sokybot.behaviors.logistics;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.navigation.api.INavigator;
import org.sokybot.navigation.api.NavigationException;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.swarm.api.TreasuryMeshBlackboardKeys;
import org.sokybot.swarm.api.TreasuryPhase;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;

/**
 * Treasury Mesh (Epic #19): walks to rendezvous within deadline.
 */
@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE, property = "order=20")
public final class PeerTradeRendezvousBehavior implements IBehavior<LogisticsSettings> {

    private static final float ARRIVAL_DISTANCE = 5.0f;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile INavigator navigator;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    @Override
    public String id() {
        return "treasury-peer-rendezvous";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return LogisticsCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<LogisticsSettings> settingsType() {
        return LogisticsSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext ctx, LogisticsSettings cfg) {
        Object sid = ctx.getPersistentData().get(TreasuryMeshBlackboardKeys.KEY_TREASURY_TRADE_SESSION_ID);
        Object ph = ctx.getPersistentData().get(TreasuryMeshBlackboardKeys.KEY_TREASURY_PHASE);
        if (!(sid instanceof String) || ((String) sid).isEmpty()) {
            return false;
        }
        return ph == TreasuryPhase.RENDEZVOUS;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext ctx, LogisticsSettings cfg) {
        long now = System.currentTimeMillis();
        Object deadlineRaw = ctx.getPersistentData().get(TreasuryMeshBlackboardKeys.KEY_TREASURY_DEADLINE_MS);
        long deadlineMs = deadlineRaw instanceof Number ? ((Number) deadlineRaw).longValue() : 0L;
        if (deadlineMs > 0L && now > deadlineMs) {
            TreasuryMeshBlackboardKeys.clearTreasurySession(ctx.getPersistentData());
            ctx.log("WARN", "Treasury rendezvous deadline exceeded for {}", ctx.getMachineId());
            return BehaviorStatus.SKIPPED;
        }

        if (navigator == null || townModel == null) {
            return BehaviorStatus.SKIPPED;
        }

        Object wpObj = ctx.getPersistentData().get(TreasuryMeshBlackboardKeys.KEY_TREASURY_RENDEZVOUS_WP);
        if (!(wpObj instanceof WorldPoint)) {
            TreasuryMeshBlackboardKeys.clearTreasurySession(ctx.getPersistentData());
            return BehaviorStatus.SKIPPED;
        }
        WorldPoint wp = (WorldPoint) wpObj;

        ITownSnapshot snap = townModel.snapshot(ctx.getMachineId()).orElse(null);
        if (snap == null) {
            return BehaviorStatus.SKIPPED;
        }

        float dx = snap.getSelfX() - wp.getX();
        float dy = snap.getSelfY() - wp.getY();
        float dz = snap.getSelfZ() - wp.getZ();
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (dist > ARRIVAL_DISTANCE) {
            try {
                navigator.walkTo(ctx, wp);
            } catch (NavigationException ex) {
                TreasuryMeshBlackboardKeys.clearTreasurySession(ctx.getPersistentData());
                ctx.log("WARN", "Treasury rendezvous walk failed: {}", ex.getMessage());
                return BehaviorStatus.SKIPPED;
            }
            return BehaviorStatus.EXECUTED;
        }

        ctx.getPersistentData().put(TreasuryMeshBlackboardKeys.KEY_TREASURY_PHASE, TreasuryPhase.HANDSHAKE);
        return BehaviorStatus.EXECUTED;
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public long postDelayMs() {
        return 350L;
    }
}
