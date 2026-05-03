package org.sokybot.behaviors.swarm.roster;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.IEngine;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.RosterBlackboardKeys;
import org.sokybot.swarm.api.SwarmDispatchEvent;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes roster rally destinations onto the assignee machine workflow blackboard (Epic #17 Phase 4).
 */
@Component(service = RosterDispatchListener.class, immediate = true)
public final class RosterDispatchListener {

    private static final Logger log = LoggerFactory.getLogger(RosterDispatchListener.class);

    private static final String PURPOSE_ROSTER_RALLY = "ROSTER_RALLY";

    @Reference
    private ISwarmEventBus swarmBus;

    @Reference
    private ISokybotContext sokybotContext;

    private volatile Disposable sub;

    @Activate
    void activate() {
        ISwarmEventBus bus = swarmBus;
        if (bus == null) {
            return;
        }
        sub = bus.observe(SwarmDispatchEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "RosterDispatchListener stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onDispatch);
    }

    @Deactivate
    void deactivate() {
        Disposable d = sub;
        sub = null;
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void onDispatch(SwarmDispatchEvent event) {
        if (event == null || event.getDestination() == null) {
            return;
        }
        if (!PURPOSE_ROSTER_RALLY.equals(event.getPurpose())) {
            return;
        }
        String assignee = event.getAssigneeMachineId();
        if (assignee == null || assignee.trim().isEmpty()) {
            return;
        }
        ISokybotContext ctx = sokybotContext;
        if (ctx == null) {
            return;
        }
        IMachineContext machine = findMachine(ctx, assignee.trim());
        if (machine == null || !machine.isRunning()) {
            return;
        }
        IEngine engine = machine.getEngine();
        if (engine == null || !engine.isRunning()) {
            return;
        }
        try {
            engine.optionalWorkflowContext().ifPresent(wf -> writeDestination(wf, event.getDestination()));
        } catch (Exception e) {
            log.debug("RosterDispatchListener assignee={}: {}", assignee, e.getMessage());
        }
    }

    private static void writeDestination(IWorkflowContext wf, WorldPoint dest) {
        if (wf == null || dest == null) {
            return;
        }
        Map<String, Object> pd = wf.getPersistentData();
        pd.put(
                RosterBlackboardKeys.KEY_ROSTER_RALLY_DESTINATION,
                new WorldPoint(dest.getX(), dest.getY(), dest.getZ()));
    }

    private static IMachineContext findMachine(ISokybotContext ctx, String fullName) {
        for (IGroupContext g : ctx.getGroups()) {
            if (g == null) {
                continue;
            }
            for (IMachineContext m : g.getMachines()) {
                if (m != null && fullName.equals(m.fullName())) {
                    return m;
                }
            }
        }
        return null;
    }
}
