package org.sokybot.behaviors.logistics;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmResourceDistressEvent;
import org.sokybot.swarm.api.TreasuryMeshBlackboardKeys;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;

/**
 * Treasury Mesh (Epic #19): publishes {@link SwarmResourceDistressEvent} when inventory falls below baseline.
 */
@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE, property = "order=10")
public final class TreasuryDistressBehavior implements IBehavior<LogisticsSettings> {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISwarmEventBus swarmBus;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISettingsRegistry settingsRegistry;

    @Override
    public String id() {
        return "treasury-distress";
    }

    @Override
    public int order() {
        return 10;
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
        if (cfg == null || swarmBus == null || townModel == null || settingsRegistry == null) {
            return false;
        }
        if (!cfg.getTreasuryBaseline().isEnabled()) {
            return false;
        }
        return townModel.snapshot(ctx.getMachineId()).filter(s -> !s.isDead()).isPresent();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext ctx, LogisticsSettings cfg) {
        LogisticsSettings settings = resolveSettings(ctx);
        if (settings == null) {
            settings = cfg;
        }
        if (settings == null || !settings.getTreasuryBaseline().isEnabled()) {
            return BehaviorStatus.SKIPPED;
        }
        if (swarmBus == null || townModel == null) {
            return BehaviorStatus.SKIPPED;
        }
        ITownSnapshot snap = townModel.snapshot(ctx.getMachineId()).orElse(null);
        if (snap == null || snap.isDead()) {
            return BehaviorStatus.SKIPPED;
        }

        @SuppressWarnings("unchecked")
        Map<Integer, Long> lastDistressTimes =
                (Map<Integer, Long>) ctx.getPersistentData().get(TreasuryMeshBlackboardKeys.KEY_LAST_DISTRESS_TIMES);
        if (lastDistressTimes == null) {
            lastDistressTimes = new ConcurrentHashMap<>();
            ctx.getPersistentData().put(TreasuryMeshBlackboardKeys.KEY_LAST_DISTRESS_TIMES, lastDistressTimes);
        }

        long now = System.currentTimeMillis();
        ResourceBaselineSettings baseline = settings.getTreasuryBaseline();
        double pct = baseline.getDistressThresholdPercent() / 100.0;

        for (Map.Entry<Integer, Integer> e : baseline.getItemTargets().entrySet()) {
            int itemRefId = e.getKey();
            int targetQty = e.getValue() == null ? 0 : e.getValue();
            if (targetQty <= 0) {
                continue;
            }
            if (now - lastDistressTimes.getOrDefault(itemRefId, 0L) < baseline.getMinOfferIntervalMs()) {
                continue;
            }

            int currentQty = snap.getInventory().countItemRef(itemRefId);
            double threshold = Math.ceil(targetQty * pct);
            if (currentQty <= threshold) {
                int deficitAmount = targetQty - currentQty;
                if (deficitAmount < 1) {
                    deficitAmount = 1;
                }
                String distressId = UUID.randomUUID().toString();
                long offerDeadlineEpochMs = now + 5000L;
                WorldPoint pos = new WorldPoint(snap.getSelfX(), snap.getSelfY(), snap.getSelfZ());

                swarmBus.publish(new SwarmResourceDistressEvent(
                        ctx.getMachineId(),
                        now,
                        distressId,
                        itemRefId,
                        deficitAmount,
                        pos,
                        offerDeadlineEpochMs));

                lastDistressTimes.put(itemRefId, now);
                ctx.log("INFO", "Treasury distress {} itemRef={} deficit={}", distressId, itemRefId, deficitAmount);
                return BehaviorStatus.EXECUTED;
            }
        }

        return BehaviorStatus.SKIPPED;
    }

    private LogisticsSettings resolveSettings(IWorkflowContext ctx) {
        try {
            ISettingsRegistry registry = settingsRegistry;
            if (registry == null) {
                return null;
            }
            ISettingsProvider<LogisticsSettings> provider = registry.getProvider(
                    ctx.getGroupName(), ctx.getMachineName(), "logistics", LogisticsSettings.class);
            return provider != null ? provider.get() : null;
        } catch (RuntimeException ex) {
            return null;
        }
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
