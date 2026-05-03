package org.sokybot.behaviors.swarm.caravan;

import java.util.Optional;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.api.MonsterRef;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.party.api.caravan.CaravanFormationSettings;
import org.sokybot.party.api.caravan.CaravanRole;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.CaravanBlackboardKeys;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmCaravanTelemetryEvent;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;

/**
 * Epic #18: Trader publishes {@link SwarmCaravanTelemetryEvent} on the swarm bus (combat cycle).
 */
@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE, property = "order=5")
public final class CaravanTelemetryPublisherBehavior implements IBehavior<CombatSettings> {

    private static final long MIN_DT_MS = 1L;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISwarmEventBus swarmBus;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ICombatModel combatModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISettingsRegistry settingsRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISokybotContext sokybotContext;

    @Override
    public String id() {
        return "caravan-telemetry-publisher";
    }

    @Override
    public int order() {
        return 5;
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
    public boolean applies(IWorkflowContext ctx, CombatSettings cfg) {
        if (sokybotContext == null || settingsRegistry == null || swarmBus == null) {
            return false;
        }
        CaravanFormationSettings s = readCaravanSettings(ctx);
        return s != null && s.isCaravanEnabled() && s.getRole() == CaravanRole.TRADER;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext ctx, CombatSettings cfg) {
        if (swarmBus == null || townModel == null || combatModel == null || settingsRegistry == null) {
            return BehaviorStatus.SKIPPED;
        }
        CaravanFormationSettings caravan = readCaravanSettings(ctx);
        if (caravan == null || !caravan.isCaravanEnabled() || caravan.getRole() != CaravanRole.TRADER) {
            return BehaviorStatus.SKIPPED;
        }

        Optional<ITownSnapshot> snapOpt = townModel.snapshot(ctx.getMachineId());
        if (!snapOpt.isPresent() || snapOpt.get().isDead()) {
            return BehaviorStatus.SKIPPED;
        }
        ITownSnapshot snap = snapOpt.get();
        WorldPoint currentPos = new WorldPoint(snap.getSelfX(), snap.getSelfY(), snap.getSelfZ());
        long now = System.currentTimeMillis();

        Object lastPosObj = ctx.getPersistentData().get(CaravanBlackboardKeys.KEY_CARAVAN_LAST_POS);
        Object lastTimeObj = ctx.getPersistentData().get(CaravanBlackboardKeys.KEY_CARAVAN_LAST_TIME);

        float velX = 0f;
        float velY = 0f;
        float velZ = 0f;

        if (lastPosObj instanceof WorldPoint && lastTimeObj instanceof Number) {
            WorldPoint lastPos = (WorldPoint) lastPosObj;
            long lastTime = ((Number) lastTimeObj).longValue();
            long dtMs = now - lastTime;
            if (dtMs >= MIN_DT_MS) {
                double dtSec = dtMs / 1000.0;
                if (dtSec > 0.0) {
                    velX = (float) ((currentPos.getX() - lastPos.getX()) / dtSec);
                    velY = (float) ((currentPos.getY() - lastPos.getY()) / dtSec);
                    velZ = (float) ((currentPos.getZ() - lastPos.getZ()) / dtSec);
                }
            }
        }

        ctx.getPersistentData().put(CaravanBlackboardKeys.KEY_CARAVAN_LAST_POS, currentPos);
        ctx.getPersistentData().put(CaravanBlackboardKeys.KEY_CARAVAN_LAST_TIME, Long.valueOf(now));

        boolean underAttack = false;
        int attackerRefId = 0;
        WorldPoint attackerApproxPos = null;

        Optional<ICombatSnapshot> cOpt = combatModel.snapshot(ctx.getMachineId());
        if (cOpt.isPresent()) {
            ICombatSnapshot combatSnap = cOpt.get();
            MonsterRef threat = pickThreat(combatSnap);
            if (threat != null) {
                underAttack = true;
                attackerRefId = threat.getRefObjId();
                attackerApproxPos = new WorldPoint(threat.getX(), threat.getY(), threat.getZ());
            }
        }

        String requestId = UUID.randomUUID().toString();
        swarmBus.publish(new SwarmCaravanTelemetryEvent(
                ctx.getMachineId(),
                now,
                requestId,
                caravan.getCaravanId(),
                currentPos,
                velX,
                velY,
                velZ,
                underAttack,
                attackerRefId,
                attackerApproxPos));

        return BehaviorStatus.EXECUTED;
    }

    /**
     * {@link MonsterRef} does not expose the mob's target entity id; treat {@link MonsterRef#isAggressiveTowardSelf()}
     * as "actively threatening the Trader" and pick the nearest such mob.
     */
    private static MonsterRef pickThreat(ICombatSnapshot snap) {
        MonsterRef best = null;
        float bestDist = Float.MAX_VALUE;
        for (MonsterRef m : snap.getNearbyMonsters()) {
            if (m == null || !m.isAggressiveTowardSelf()) {
                continue;
            }
            float d = m.getDistanceToSelf();
            if (d < bestDist) {
                bestDist = d;
                best = m;
            }
        }
        return best;
    }

    private CaravanFormationSettings readCaravanSettings(IWorkflowContext ctx) {
        ISettingsRegistry reg = settingsRegistry;
        if (reg == null) {
            return null;
        }
        try {
            ISettingsProvider<CaravanFormationSettings> p = reg.getProvider(
                    ctx.getGroupName(), ctx.getMachineName(), "caravan", CaravanFormationSettings.class);
            return p != null ? p.get() : null;
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
        return 400L;
    }
}
