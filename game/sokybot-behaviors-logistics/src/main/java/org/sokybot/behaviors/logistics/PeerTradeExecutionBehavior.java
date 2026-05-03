package org.sokybot.behaviors.logistics;

import java.util.List;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.trade.packets.TradePackets;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.IPlayer;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.swarm.api.TreasuryMeshBlackboardKeys;
import org.sokybot.swarm.api.TreasuryPhase;
import org.sokybot.swarm.api.TreasuryRole;
import org.sokybot.town.projections.api.ITownModel;

/**
 * Treasury Mesh (Epic #19): exchange handshake and placeholder completion until trade coordination is wired.
 */
@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE, property = "order=21")
public final class PeerTradeExecutionBehavior implements IBehavior<LogisticsSettings> {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    @Override
    public String id() {
        return "treasury-peer-trade-execution";
    }

    @Override
    public int order() {
        return 21;
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
        if (!(ph instanceof TreasuryPhase)) {
            return false;
        }
        TreasuryPhase phase = (TreasuryPhase) ph;
        return phase == TreasuryPhase.HANDSHAKE || phase == TreasuryPhase.TRADE_ACTIVE;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext ctx, LogisticsSettings cfg) {
        long now = System.currentTimeMillis();
        Object deadlineRaw = ctx.getPersistentData().get(TreasuryMeshBlackboardKeys.KEY_TREASURY_DEADLINE_MS);
        long deadlineMs = deadlineRaw instanceof Number ? ((Number) deadlineRaw).longValue() : 0L;
        if (deadlineMs > 0L && now > deadlineMs) {
            TreasuryMeshBlackboardKeys.clearTreasurySession(ctx.getPersistentData());
            ctx.log("WARN", "Treasury trade deadline exceeded for {}", ctx.getMachineId());
            return BehaviorStatus.SKIPPED;
        }

        Object phObj = ctx.getPersistentData().get(TreasuryMeshBlackboardKeys.KEY_TREASURY_PHASE);
        if (!(phObj instanceof TreasuryPhase)) {
            TreasuryMeshBlackboardKeys.clearTreasurySession(ctx.getPersistentData());
            return BehaviorStatus.SKIPPED;
        }
        TreasuryPhase phase = (TreasuryPhase) phObj;

        Object roleObj = ctx.getPersistentData().get(TreasuryMeshBlackboardKeys.KEY_TREASURY_ROLE);
        TreasuryRole role = roleObj instanceof TreasuryRole ? (TreasuryRole) roleObj : null;

        String sessionId = (String) ctx.getPersistentData().get(TreasuryMeshBlackboardKeys.KEY_TREASURY_TRADE_SESSION_ID);

        if (phase == TreasuryPhase.HANDSHAKE) {
            if (role == TreasuryRole.SUPPLIER) {
                Object peerMachineObj =
                        ctx.getPersistentData().get(TreasuryMeshBlackboardKeys.KEY_TREASURY_PEER_MACHINE_ID);
                String peerMachineId =
                        peerMachineObj instanceof String ? (String) peerMachineObj : "";
                if (townModel != null && !peerMachineId.isEmpty()) {
                    townModel.snapshot(peerMachineId);
                }

                Object wpObj = ctx.getPersistentData().get(TreasuryMeshBlackboardKeys.KEY_TREASURY_RENDEZVOUS_WP);
                WorldPoint near = wpObj instanceof WorldPoint ? (WorldPoint) wpObj : null;
                Optional<Integer> targetSpawn =
                        near != null ? findNearestOtherPlayerUid(ctx, near) : Optional.empty();
                if (targetSpawn.isPresent()) {
                    TradePackets.sendExchangeRequest(ctx, targetSpawn.get().longValue());
                    ctx.log("INFO", "Treasury supplier sent exchange request to spawn {}", targetSpawn.get());
                    ctx.getPersistentData()
                            .put(TreasuryMeshBlackboardKeys.KEY_TREASURY_PHASE, TreasuryPhase.TRADE_ACTIVE);
                } else {
                    ctx.log(
                            "WARN",
                            "Treasury supplier could not resolve peer player uid for session {}; handshake deferred",
                            sessionId);
                }
                return BehaviorStatus.EXECUTED;
            }
            if (role == TreasuryRole.DISTRESSED) {
                return BehaviorStatus.EXECUTED;
            }
            TreasuryMeshBlackboardKeys.clearTreasurySession(ctx.getPersistentData());
            return BehaviorStatus.SKIPPED;
        }

        if (phase == TreasuryPhase.TRADE_ACTIVE) {
            ctx.log(
                    "INFO",
                    "Treasury trade active for session [{}]. Handing off to trade coordination.",
                    sessionId != null ? sessionId : "?");
            TreasuryMeshBlackboardKeys.clearTreasurySession(ctx.getPersistentData());
            return BehaviorStatus.EXECUTED;
        }

        return BehaviorStatus.SKIPPED;
    }

    /**
     * Best-effort: nearest other {@link IPlayer} to rendezvous (town overlay does not expose peer spawn ids).
     */
    private static Optional<Integer> findNearestOtherPlayerUid(IWorkflowContext ctx, WorldPoint near) {
        IGameModel gm = ctx.getGameModel();
        ITrainer trainer = gm.getTrainer();
        int selfUid = trainer.getUniqueId();
        List<IPlayer> players = gm.snapshotAll(IPlayer.class);
        double best = Double.MAX_VALUE;
        Integer bestUid = null;
        for (IPlayer p : players) {
            if (p == null || p.getUniqueId() == selfUid) {
                continue;
            }
            GamePosition pos = p.getPosition();
            if (pos == null) {
                continue;
            }
            double dx = pos.getX() - near.getX();
            double dy = pos.getY() - near.getY();
            double dz = pos.getZ() - near.getZ();
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < best) {
                best = d2;
                bestUid = p.getUniqueId();
            }
        }
        return Optional.ofNullable(bestUid);
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
