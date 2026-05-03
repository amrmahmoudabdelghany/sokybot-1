package org.sokybot.behaviors.swarm.caravan;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.navigation.api.INavigator;
import org.sokybot.navigation.api.NavigationException;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.party.api.caravan.CaravanFormationSettings;
import org.sokybot.party.api.caravan.CaravanRole;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.CaravanBlackboardKeys;
import org.sokybot.swarm.api.SwarmCaravanTelemetryEvent;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;

/**
 * Epic #18: Hunter maintains formation offset around Trader telemetry (lookahead + radial offset).
 */
@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE, property = "order=6")
public final class FormationEscortBehavior implements IBehavior<CombatSettings> {

    private static final float LOOKAHEAD_SEC = 0.5f;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ICaravanTelemetryCache caravanTelemetryCache;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile INavigator navigator;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISettingsRegistry settingsRegistry;

    @Override
    public String id() {
        return "caravan-formation-escort";
    }

    @Override
    public int order() {
        return 6;
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
        if (caravanTelemetryCache == null || settingsRegistry == null) {
            return false;
        }
        CaravanFormationSettings settings = readCaravanSettings(ctx);
        if (settings == null || !settings.isCaravanEnabled() || settings.getRole() != CaravanRole.HUNTER) {
            return false;
        }
        Optional<SwarmCaravanTelemetryEvent> tel =
                caravanTelemetryCache.getLatest(settings.getCaravanId());
        if (!tel.isPresent()) {
            return false;
        }
        SwarmCaravanTelemetryEvent event = tel.get();
        return !event.isUnderAttack();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext ctx, CombatSettings cfg) {
        if (navigator == null || townModel == null || caravanTelemetryCache == null || settingsRegistry == null) {
            return BehaviorStatus.SKIPPED;
        }
        CaravanFormationSettings settings = readCaravanSettings(ctx);
        if (settings == null || !settings.isCaravanEnabled() || settings.getRole() != CaravanRole.HUNTER) {
            return BehaviorStatus.SKIPPED;
        }
        Optional<SwarmCaravanTelemetryEvent> tel =
                caravanTelemetryCache.getLatest(settings.getCaravanId());
        if (!tel.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        SwarmCaravanTelemetryEvent event = tel.get();
        if (event.isUnderAttack()) {
            return BehaviorStatus.SKIPPED;
        }

        Optional<ITownSnapshot> selfOpt = townModel.snapshot(ctx.getMachineId());
        if (!selfOpt.isPresent() || selfOpt.get().isDead()) {
            return BehaviorStatus.SKIPPED;
        }
        ITownSnapshot self = selfOpt.get();

        WorldPoint traderPos = event.getTraderPosition();
        float predictedX = traderPos.getX() + event.getVelX() * LOOKAHEAD_SEC;
        float predictedY = traderPos.getY() + event.getVelY() * LOOKAHEAD_SEC;
        float predictedZ = traderPos.getZ() + event.getVelZ() * LOOKAHEAD_SEC;

        double radians = Math.toRadians(settings.getFormationAngleDegrees());
        float radius = settings.getFormationRadiusWorld();
        float offsetX = (float) (radius * Math.cos(radians));
        float offsetY = (float) (radius * Math.sin(radians));

        float targetX = predictedX + offsetX;
        float targetY = predictedY + offsetY;
        float targetZ = predictedZ;
        WorldPoint targetPoint = new WorldPoint(targetX, targetY, targetZ);

        float sx = self.getSelfX();
        float sy = self.getSelfY();
        float sz = self.getSelfZ();
        float dx = sx - targetPoint.getX();
        float dy = sy - targetPoint.getY();
        float dz = sz - targetPoint.getZ();
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < settings.getRepositionEpsilonWorld()) {
            return BehaviorStatus.EXECUTED;
        }

        long now = System.currentTimeMillis();
        Object lastMoveRaw = ctx.getPersistentData().get(CaravanBlackboardKeys.KEY_CARAVAN_LAST_MOVE_TIME);
        long lastMove = lastMoveRaw instanceof Number ? ((Number) lastMoveRaw).longValue() : 0L;
        if (lastMove > 0L && now - lastMove < settings.getMinMoveIntervalMs()) {
            return BehaviorStatus.EXECUTED;
        }

        try {
            navigator.walkTo(ctx, targetPoint);
        } catch (NavigationException ex) {
            ctx.log("WARN", "Formation escort walk failed: {}", ex.getMessage());
            return BehaviorStatus.SKIPPED;
        }

        ctx.getPersistentData().put(CaravanBlackboardKeys.KEY_CARAVAN_LAST_MOVE_TIME, Long.valueOf(now));
        return BehaviorStatus.EXECUTED;
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
