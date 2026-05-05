package org.sokybot.behaviors.combat.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.navigation.api.INavigator;
import org.sokybot.navigation.api.NavigationException;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.warroom.SwarmFormationCommandEvent;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epic #25 Phase 4: consumes formation commands from the swarm bus and walks bots into grid holds.
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE, property = "order=1")
public final class FormationHoldBehavior implements IBehavior<CombatSettings> {

    private static final Logger log = LoggerFactory.getLogger(FormationHoldBehavior.class);

    private static final double HOLD_EPSILON = 3.0;

    private static final ConcurrentHashMap<String, SwarmFormationCommandEvent> pendingCommands = new ConcurrentHashMap<>();

    private static volatile Disposable formationSub;

    private static volatile boolean swarmFormationBusHooked;

    private static final AtomicInteger prototypeRefCount = new AtomicInteger();

    private static final Object BUS_LOCK = new Object();

    private static final String BEHAVIOR_ID = "formationHold";

    @Reference
    private ISwarmEventBus swarmEventBus;

    @Reference
    private INavigator navigator;

    @Activate
    void activate() {
        if (prototypeRefCount.incrementAndGet() != 1) {
            return;
        }
        synchronized (BUS_LOCK) {
            if (swarmFormationBusHooked) {
                return;
            }
            ISwarmEventBus bus = swarmEventBus;
            if (bus == null) {
                log.warn("FormationHoldBehavior: ISwarmEventBus unavailable");
                prototypeRefCount.decrementAndGet();
                return;
            }
            formationSub = bus.observe(SwarmFormationCommandEvent.class)
                    .onErrorContinue((err, ev) -> log.warn(
                            "FormationHoldBehavior formation stream: {}",
                            err != null ? err.getMessage() : "unknown"))
                    .subscribe(FormationHoldBehavior::handleFormationDispatch);
            swarmFormationBusHooked = true;
        }
    }

    @Deactivate
    void deactivate() {
        if (prototypeRefCount.decrementAndGet() > 0) {
            return;
        }
        synchronized (BUS_LOCK) {
            Disposable d = formationSub;
            formationSub = null;
            swarmFormationBusHooked = false;
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
            pendingCommands.clear();
        }
    }

    private static void handleFormationDispatch(SwarmFormationCommandEvent event) {
        if (event == null) {
            return;
        }
        try {
            pendingCommands.put(event.getTargetMachineId(), event);
        } catch (Exception ex) {
            log.warn("FormationHoldBehavior: index formation command failed: {}", ex.getMessage());
        }
    }

    @Override
    public String id() {
        return BEHAVIOR_ID;
    }

    @Override
    public int order() {
        return 1;
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
        if (context == null || settings == null) {
            return false;
        }
        String machineId = context.getMachineId();
        if (!pendingCommands.containsKey(machineId)) {
            return false;
        }
        return isIdleOrHunting(context);
    }

    private static boolean isIdleOrHunting(IWorkflowContext context) {
        Map<String, Object> pd = context.getPersistentData();
        Object phase = pd != null ? pd.get(CombatCycleKeys.KEY_COMBAT_PHASE) : null;
        if (phase == null) {
            return true;
        }
        String p = String.valueOf(phase);
        return CombatCycleKeys.PHASE_HUNTING.equals(p)
                || CombatCycleKeys.PHASE_IDLE.equals(p);
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        String machineId = context.getMachineId();
        SwarmFormationCommandEvent evt = pendingCommands.get(machineId);
        if (evt == null) {
            return BehaviorStatus.SKIPPED;
        }

        INavigator nav = navigator;
        if (nav == null || context.getGameModel() == null || context.getGameModel().getTrainer() == null) {
            return BehaviorStatus.SKIPPED;
        }

        GamePosition pos = context.getGameModel().getTrainer().getPosition();
        if (pos == null) {
            return BehaviorStatus.SKIPPED;
        }

        double dx = pos.getX() - evt.getHoldX();
        double dy = pos.getY() - evt.getHoldY();
        double distance = Math.hypot(dx, dy);

        if (distance > HOLD_EPSILON) {
            try {
                nav.walkTo(context, new WorldPoint((float) evt.getHoldX(), (float) evt.getHoldY(), pos.getZ()));
                pendingCommands.remove(machineId);
            } catch (NavigationException ex) {
                log.debug("FormationHoldBehavior walkTo: {}", ex.getMessage());
                return BehaviorStatus.SKIPPED;
            }
            return BehaviorStatus.EXECUTED;
        }

        return BehaviorStatus.SKIPPED;
    }
}
