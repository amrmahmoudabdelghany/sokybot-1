package org.sokybot.behaviors.combat.internal.sentinel;

import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.CombatPackets;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.behaviors.combat.internal.settings.SentinelSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.IPlayer;
import org.sokybot.gamemodel.model.ITrainer;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.sentinel.IThreatLedgerCache;

/**
 * Epic #20 Phase 3: PvP sentinel override — lock onto ledger-listed hostile players in range before Phalanx/Caravan.
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE, property = "order=3")
public final class SentinelEngageBehavior implements IBehavior<CombatSettings> {

    private static final float MAX_ENGAGE_DISTANCE = 50.0f;

    @Reference
    private ISettingsRegistry settingsRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IThreatLedgerCache threatLedgerCache;

    @Override
    public String id() {
        return "combat.sentinelEngage";
    }

    @Override
    public int order() {
        return 3;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return CombatCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<CombatSettings> settingsType() {
        return CombatSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, CombatSettings settings) {
        IThreatLedgerCache ledger = threatLedgerCache;
        ISettingsRegistry registry = settingsRegistry;
        if (ledger == null || registry == null) {
            return false;
        }
        SentinelSettings sentinel = readSentinel(context, registry);
        if (sentinel == null || !sentinel.isSentinelEnabled()) {
            return false;
        }
        if (ledger.getActiveHostiles(System.currentTimeMillis()).isEmpty()) {
            return false;
        }
        IGameModel gm = context.getGameModel();
        if (gm == null || gm.getTrainer() == null) {
            return false;
        }
        return true;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        IThreatLedgerCache ledger = threatLedgerCache;
        ISettingsRegistry registry = settingsRegistry;
        if (ledger == null || registry == null) {
            return BehaviorStatus.SKIPPED;
        }
        SentinelSettings sentinel = readSentinel(context, registry);
        if (sentinel == null || !sentinel.isSentinelEnabled()) {
            return BehaviorStatus.SKIPPED;
        }

        Set<String> activeHostiles = ledger.getActiveHostiles(System.currentTimeMillis());
        if (activeHostiles.isEmpty()) {
            return BehaviorStatus.SKIPPED;
        }

        IGameModel gameModel = context.getGameModel();
        if (gameModel == null) {
            return BehaviorStatus.SKIPPED;
        }
        ITrainer self = gameModel.getTrainer();
        if (self == null) {
            return BehaviorStatus.SKIPPED;
        }

        List<IPlayer> players = gameModel.snapshotAll(IPlayer.class);
        if (players == null || players.isEmpty()) {
            return BehaviorStatus.SKIPPED;
        }

        IPlayer target = null;
        double bestDist = Double.MAX_VALUE;
        int selfId = self.getUniqueId();

        for (IPlayer player : players) {
            if (player == null || player.getUniqueId() == selfId) {
                continue;
            }
            if (player.getCurrentHP() <= 0) {
                continue;
            }
            String rawName = player.getName();
            if (rawName == null) {
                continue;
            }
            String normalized = rawName.trim().toLowerCase();
            if (normalized.isEmpty() || !activeHostiles.contains(normalized)) {
                continue;
            }
            double d = self.distance(player.getX(), player.getY());
            if (d <= MAX_ENGAGE_DISTANCE && d < bestDist) {
                bestDist = d;
                target = player;
            }
        }

        if (target == null) {
            return BehaviorStatus.SKIPPED;
        }

        if (self.getTargetId() != target.getUniqueId()) {
            CombatPackets.sendSelectEntity(context, target.getUniqueId());
        }
        return BehaviorStatus.EXECUTED;
    }

    @Override
    public long postDelayMs() {
        return 200L;
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    private static SentinelSettings readSentinel(IWorkflowContext ctx, ISettingsRegistry registry) {
        try {
            ISettingsProvider<SentinelSettings> p = registry.getProvider(
                    ctx.getGroupName(),
                    ctx.getMachineName(),
                    "sentinel",
                    SentinelSettings.class);
            return p != null ? p.get() : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
