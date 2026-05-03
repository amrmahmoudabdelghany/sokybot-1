package org.sokybot.behaviors.swarm.treasury;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.IEngine;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.SwarmTradeDispatchEvent;
import org.sokybot.swarm.api.TreasuryMeshBlackboardKeys;
import org.sokybot.swarm.api.TreasuryPhase;
import org.sokybot.swarm.api.TreasuryRole;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Treasury Mesh (Epic #19): applies {@link SwarmTradeDispatchEvent} to the target machine's workflow blackboard.
 */
@Component(service = TreasuryDispatchListener.class, immediate = true)
public final class TreasuryDispatchListener {

    private static final Logger log = LoggerFactory.getLogger(TreasuryDispatchListener.class);

    @Reference
    private ISwarmEventBus swarmBus;

    @Reference
    private ISokybotContext sokybotContext;

    private volatile Disposable dispatchSub;

    @Activate
    void activate() {
        ISwarmEventBus bus = swarmBus;
        if (bus == null) {
            log.warn("TreasuryDispatchListener: ISwarmEventBus unavailable");
            return;
        }
        dispatchSub = bus.observe(SwarmTradeDispatchEvent.class)
                .onErrorContinue((err, trigger) -> log.warn(
                        "TreasuryDispatchListener dispatch stream: {}",
                        err != null ? err.getMessage() : "unknown"))
                .subscribe(this::onDispatch);
    }

    @Deactivate
    void deactivate() {
        Disposable d = dispatchSub;
        dispatchSub = null;
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void onDispatch(SwarmTradeDispatchEvent event) {
        if (event == null) {
            return;
        }
        String assignee = event.getAssigneeMachineId();
        if (assignee == null || assignee.isEmpty()) {
            return;
        }
        ISokybotContext ctx = sokybotContext;
        if (ctx == null) {
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
                    maybeApplyDispatch(event, assignee, machine);
                } catch (Exception ex) {
                    log.trace(
                            "TreasuryDispatchListener skip machine {}: {}",
                            machine.fullName(),
                            ex.getMessage());
                }
            }
        }
    }

    private static void maybeApplyDispatch(SwarmTradeDispatchEvent event, String assignee, IMachineContext machine) {
        if (!assignee.equals(machine.fullName())) {
            return;
        }
        if (!machine.isRunning()) {
            return;
        }
        IEngine engine = machine.getEngine();
        if (engine == null) {
            return;
        }
        IWorkflowContext wf =
                engine.optionalWorkflowContext().orElse(null);
        if (wf == null) {
            return;
        }

        TreasuryRole role = event.getAssigneeMachineId().equals(event.getSupplierMachineId())
                ? TreasuryRole.SUPPLIER
                : TreasuryRole.DISTRESSED;

        String peerMachineId = role == TreasuryRole.SUPPLIER
                ? event.getDistressedMachineId()
                : event.getSupplierMachineId();

        Map<String, Object> pd = wf.getPersistentData();
        pd.put(TreasuryMeshBlackboardKeys.KEY_TREASURY_TRADE_SESSION_ID, event.getTradeSessionId());
        pd.put(TreasuryMeshBlackboardKeys.KEY_TREASURY_RENDEZVOUS_WP, event.getRendezvous());
        pd.put(TreasuryMeshBlackboardKeys.KEY_TREASURY_PEER_MACHINE_ID, peerMachineId);
        pd.put(TreasuryMeshBlackboardKeys.KEY_TREASURY_ROLE, role);
        pd.put(TreasuryMeshBlackboardKeys.KEY_TREASURY_PHASE, TreasuryPhase.RENDEZVOUS);
        pd.put(TreasuryMeshBlackboardKeys.KEY_TREASURY_ITEM_REF_ID, event.getItemRefId());
        pd.put(TreasuryMeshBlackboardKeys.KEY_TREASURY_COMMITTED_AMOUNT, event.getCommittedAmount());
        pd.put(TreasuryMeshBlackboardKeys.KEY_TREASURY_DEADLINE_MS, event.getRendezvousDeadlineEpochMs());

        log.info(
                "TreasuryDispatchListener: session {} assignee={} role={} peer={}",
                event.getTradeSessionId(),
                assignee,
                role,
                peerMachineId);
    }
}
