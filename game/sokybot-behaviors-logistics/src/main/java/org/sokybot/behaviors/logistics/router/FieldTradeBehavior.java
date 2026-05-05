package org.sokybot.behaviors.logistics.router;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.logistics.LogisticsCycleKeys;
import org.sokybot.behaviors.logistics.LogisticsSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.router.SwarmTradeCommandEvent;

import reactor.core.Disposable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epic #24 Phase 4: placeholder execution for router-emitted peer trades (logistics cycle).
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE, property = "order=3")
public final class FieldTradeBehavior implements IBehavior<LogisticsSettings> {

    private static final Logger log = LoggerFactory.getLogger(FieldTradeBehavior.class);

    /** Shared across prototype {@link IBehavior} instances for the same swarm-wide trade queue. */
    private static final ConcurrentHashMap<String, SwarmTradeCommandEvent> pendingTrades =
            new ConcurrentHashMap<>();

    private static volatile ISokybotContext activeSokybotContext;

    private static volatile Disposable tradeSub;

    private static volatile boolean swarmTradeBusHooked;

    private static final AtomicInteger prototypeRefCount = new AtomicInteger();

    private static final Object BUS_LOCK = new Object();

    private static final String BEHAVIOR_ID = "logistics.fieldTradeRouter";

    @Reference
    private ISwarmEventBus swarmEventBus;

    @Reference
    private ISokybotContext sokybotContext;

    @Activate
    void activate() {
        activeSokybotContext = sokybotContext;
        if (prototypeRefCount.incrementAndGet() != 1) {
            return;
        }
        synchronized (BUS_LOCK) {
            if (swarmTradeBusHooked) {
                return;
            }
            ISwarmEventBus bus = swarmEventBus;
            if (bus == null) {
                log.warn("FieldTradeBehavior: ISwarmEventBus unavailable");
                prototypeRefCount.decrementAndGet();
                return;
            }
            tradeSub = bus.observe(SwarmTradeCommandEvent.class)
                    .onErrorContinue((err, trigger) -> log.warn(
                            "FieldTradeBehavior trade stream: {}",
                            err != null ? err.getMessage() : "unknown"))
                    .subscribe(FieldTradeBehavior::handleTradeDispatch);
            swarmTradeBusHooked = true;
        }
    }

    @Deactivate
    void deactivate() {
        if (prototypeRefCount.decrementAndGet() > 0) {
            return;
        }
        synchronized (BUS_LOCK) {
            Disposable d = tradeSub;
            tradeSub = null;
            swarmTradeBusHooked = false;
            activeSokybotContext = null;
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
            pendingTrades.clear();
        }
    }

    private static void handleTradeDispatch(SwarmTradeCommandEvent event) {
        if (event == null) {
            return;
        }
        ISokybotContext ctx = activeSokybotContext;
        if (ctx == null) {
            return;
        }
        try {
            String from = event.getFromMachineId();
            String to = event.getToMachineId();
            for (IGroupContext g : ctx.getGroups()) {
                if (g == null) {
                    continue;
                }
                for (IMachineContext m : g.getMachines()) {
                    if (m != null && m.isRunning()) {
                        String id = m.fullName();
                        if (Objects.equals(id, from) || Objects.equals(id, to)) {
                            pendingTrades.put(event.getTradeSessionId(), event);
                            return;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("FieldTradeBehavior: index trade command failed: {}", ex.getMessage());
        }
    }

    @Override
    public String id() {
        return BEHAVIOR_ID;
    }

    @Override
    public int order() {
        return 3;
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
    public boolean applies(IWorkflowContext context, LogisticsSettings settings) {
        if (pendingTrades.isEmpty()) {
            return false;
        }
        String me = context.getMachineId();
        for (SwarmTradeCommandEvent t : pendingTrades.values()) {
            if (t != null && (Objects.equals(me, t.getFromMachineId()) || Objects.equals(me, t.getToMachineId()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, LogisticsSettings settings) {
        String me = context.getMachineId();
        Iterator<Map.Entry<String, SwarmTradeCommandEvent>> it = pendingTrades.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SwarmTradeCommandEvent> en = it.next();
            SwarmTradeCommandEvent trade = en.getValue();
            if (trade == null) {
                it.remove();
                continue;
            }
            if (!Objects.equals(me, trade.getFromMachineId()) && !Objects.equals(me, trade.getToMachineId())) {
                continue;
            }
            context.log(
                    "INFO",
                    "Executing trade: {} from {} to {}",
                    trade.getUniqueItemId(),
                    trade.getFromMachineId(),
                    trade.getToMachineId());
            RouterTradePackets.sendTradeRequest(context, trade.getTargetCharacterName());
            RouterTradePackets.sendTradeAddItem(context, (byte) trade.getSlotIndex());
            RouterTradePackets.sendTradeConfirm(context);
            it.remove();
            return BehaviorStatus.EXECUTED;
        }
        return BehaviorStatus.SKIPPED;
    }
}
