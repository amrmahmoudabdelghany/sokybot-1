package org.sokybot.behaviors.swarm.caravan;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.IEngine;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.party.api.caravan.CaravanFormationSettings;
import org.sokybot.party.api.caravan.CaravanRole;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;
import org.sokybot.swarm.api.CaravanBlackboardKeys;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmCaravanTelemetryEvent;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epic #18: pushes Trader threat hints onto Hunter workflow persistent data when telemetry reports under attack.
 */
@Component(immediate = true)
public final class CaravanThreatBridgeListener {

    private static final long DEFEND_WINDOW_MS = 5000L;

    private static final Logger log = LoggerFactory.getLogger(CaravanThreatBridgeListener.class);

    @Reference
    private ISwarmEventBus swarmBus;

    @Reference
    private ISokybotContext sokybotContext;

    @Reference
    private ISettingsRegistry settingsRegistry;

    private volatile Disposable telemetrySub;

    @Activate
    void activate() {
        ISwarmEventBus bus = swarmBus;
        if (bus == null) {
            log.warn("CaravanThreatBridgeListener: ISwarmEventBus unavailable");
            return;
        }
        telemetrySub = bus.observe(SwarmCaravanTelemetryEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "CaravanThreatBridgeListener stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onTelemetry);
    }

    @Deactivate
    void deactivate() {
        Disposable d = telemetrySub;
        telemetrySub = null;
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void onTelemetry(SwarmCaravanTelemetryEvent event) {
        if (event == null || !event.isUnderAttack()) {
            return;
        }
        ISokybotContext ctx = sokybotContext;
        ISettingsRegistry registry = settingsRegistry;
        if (ctx == null || registry == null) {
            return;
        }
        String eventCaravanId = event.getCaravanId();
        if (eventCaravanId == null || eventCaravanId.trim().isEmpty()) {
            return;
        }

        for (IGroupContext group : ctx.getGroups()) {
            if (group == null) {
                continue;
            }
            for (IMachineContext machine : group.getMachines()) {
                if (machine == null) {
                    continue;
                }
                try {
                    maybeArmThreat(registry, event, eventCaravanId.trim(), machine);
                } catch (Exception ex) {
                    log.trace(
                            "CaravanThreatBridgeListener skip machine {}: {}",
                            machine.fullName(),
                            ex.getMessage());
                }
            }
        }
    }

    private static void maybeArmThreat(
            ISettingsRegistry registry,
            SwarmCaravanTelemetryEvent event,
            String eventCaravanId,
            IMachineContext machine) {
        if (!machine.isRunning()) {
            return;
        }
        CaravanFormationSettings settings = readCaravan(registry, machine);
        if (settings == null || !settings.isCaravanEnabled() || settings.getRole() != CaravanRole.HUNTER) {
            return;
        }
        String cid = settings.getCaravanId();
        if (cid == null || cid.trim().isEmpty() || !cid.trim().equals(eventCaravanId)) {
            return;
        }

        IEngine engine = machine.getEngine();
        if (engine == null) {
            return;
        }
        IWorkflowContext wf = engine.optionalWorkflowContext().orElse(null);
        if (wf == null) {
            return;
        }

        Map<String, Object> pd = wf.getPersistentData();
        pd.put(CaravanBlackboardKeys.KEY_THREAT_REF_ID, Integer.valueOf(event.getAttackerRefId()));
        pd.put(CaravanBlackboardKeys.KEY_THREAT_POS, event.getAttackerApproxPosition());
        pd.put(CaravanBlackboardKeys.KEY_DEFEND_UNTIL_MS, Long.valueOf(System.currentTimeMillis() + DEFEND_WINDOW_MS));
    }

    private static CaravanFormationSettings readCaravan(ISettingsRegistry registry, IMachineContext machine) {
        try {
            ISettingsProvider<CaravanFormationSettings> p = registry.getProvider(
                    machine.getGroupName(),
                    machine.getMachineName(),
                    "caravan",
                    CaravanFormationSettings.class);
            return p != null ? p.get() : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
