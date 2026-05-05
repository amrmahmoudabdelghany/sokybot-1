package org.sokybot.behaviors.logistics.router;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
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
 * Epic #24 Phase 4: rate-limited trade handshake for router-emitted peer trades (logistics cycle).
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE, property = "order=3")
public final class FieldTradeBehavior implements IBehavior<LogisticsSettings> {

    private static final Logger log = LoggerFactory.getLogger(FieldTradeBehavior.class);

    private static final long ACTION_EPSILON_MS = 1000L;
    private static final long AWAIT_TIMEOUT_MS = 10_000L;

    /** Shared across prototype {@link IBehavior} instances for the same swarm-wide trade queue. */
    private static final ConcurrentMap<String, ActiveTradeState> pendingTrades = new ConcurrentHashMap<>();

    private static volatile ISokybotContext activeSokybotContext;

    private static volatile Disposable tradeSub;

    private static volatile boolean swarmTradeBusHooked;

    private static final AtomicInteger prototypeRefCount = new AtomicInteger();

    private static final Object BUS_LOCK = new Object();

    private static final String BEHAVIOR_ID = "logistics.fieldTradeRouter";

    private static final class ActiveTradeState {
        volatile SwarmTradeCommandEvent command;
        volatile TradeProtocolState state;
        volatile long lastActionTimeMs;

        ActiveTradeState(SwarmTradeCommandEvent command, TradeProtocolState state, long lastActionTimeMs) {
            this.command = command;
            this.state = state;
            this.lastActionTimeMs = lastActionTimeMs;
        }
    }

    /**
     * Called from inbound {@link org.sokybot.gameevents.events.core.IPacketTranslator}s (trade server opcodes).
     */
    static void notifyInboundTradeOpcode(String machineFullName, int opcode) {
        TradeProtocolState newState = mapInboundOpcode(opcode);
        if (newState == null) {
            return;
        }
        for (Map.Entry<String, ActiveTradeState> e : pendingTrades.entrySet()) {
            ActiveTradeState ats = e.getValue();
            if (ats == null || ats.command == null) {
                continue;
            }
            SwarmTradeCommandEvent cmd = ats.command;
            if (!Objects.equals(machineFullName, cmd.getFromMachineId())
                    && !Objects.equals(machineFullName, cmd.getToMachineId())) {
                continue;
            }
            pendingTrades.computeIfPresent(e.getKey(), (id, cur) -> {
                cur.state = newState;
                cur.lastActionTimeMs = System.currentTimeMillis();
                return cur;
            });
            return;
        }
    }

    private static TradeProtocolState mapInboundOpcode(int opcode) {
        switch (opcode) {
            case 0xB081:
                return TradeProtocolState.ADDING_ITEM;
            case 0xB082:
                return TradeProtocolState.CONFIRMING;
            case 0xB083:
                return TradeProtocolState.APPROVING;
            case 0xB084:
                return TradeProtocolState.DONE;
            default:
                return null;
        }
    }

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
                            pendingTrades.put(
                                    event.getTradeSessionId(),
                                    new ActiveTradeState(event, TradeProtocolState.INIT, 0L));
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
        for (ActiveTradeState ats : pendingTrades.values()) {
            if (ats != null
                    && ats.command != null
                    && Objects.equals(me, ats.command.getFromMachineId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, LogisticsSettings settings) {
        String me = context.getMachineId();
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, ActiveTradeState>> it = pendingTrades.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ActiveTradeState> en = it.next();
            String sessionId = en.getKey();
            ActiveTradeState ats = en.getValue();
            if (ats == null || ats.command == null) {
                it.remove();
                continue;
            }
            SwarmTradeCommandEvent trade = ats.command;
            if (!Objects.equals(me, trade.getFromMachineId())) {
                continue;
            }

            TradeProtocolState st = ats.state;

            if (st != TradeProtocolState.INIT && (now - ats.lastActionTimeMs <= ACTION_EPSILON_MS)) {
                return BehaviorStatus.SKIPPED;
            }

            switch (st) {
                case DONE:
                case ABORTED:
                    pendingTrades.remove(sessionId);
                    return BehaviorStatus.SKIPPED;

                case AWAITING_ACCEPT:
                case AWAITING_ITEM_ECHO:
                case AWAITING_CONFIRM:
                case AWAITING_APPROVE:
                    if (now - ats.lastActionTimeMs > AWAIT_TIMEOUT_MS) {
                        pendingTrades.computeIfPresent(sessionId, (id, cur) -> {
                            cur.state = TradeProtocolState.ABORTED;
                            cur.lastActionTimeMs = now;
                            return cur;
                        });
                    }
                    return BehaviorStatus.SKIPPED;

                case INIT:
                    context.log(
                            "INFO",
                            "Trade handshake INIT: {} from {} to {}",
                            trade.getUniqueItemId(),
                            trade.getFromMachineId(),
                            trade.getToMachineId());
                    if (!RouterTradePackets.sendTradeRequest(context, trade.getTargetCharacterName())) {
                        pendingTrades.computeIfPresent(sessionId, (id, cur) -> {
                            cur.state = TradeProtocolState.ABORTED;
                            cur.lastActionTimeMs = now;
                            return cur;
                        });
                        return BehaviorStatus.EXECUTED;
                    }
                    pendingTrades.computeIfPresent(sessionId, (id, cur) -> {
                        cur.state = TradeProtocolState.AWAITING_ACCEPT;
                        cur.lastActionTimeMs = now;
                        return cur;
                    });
                    return BehaviorStatus.EXECUTED;

                case ADDING_ITEM:
                    RouterTradePackets.sendTradeAddItem(context, (byte) trade.getSlotIndex());
                    pendingTrades.computeIfPresent(sessionId, (id, cur) -> {
                        cur.state = TradeProtocolState.AWAITING_ITEM_ECHO;
                        cur.lastActionTimeMs = now;
                        return cur;
                    });
                    return BehaviorStatus.EXECUTED;

                case CONFIRMING:
                    RouterTradePackets.sendTradeConfirm(context);
                    pendingTrades.computeIfPresent(sessionId, (id, cur) -> {
                        cur.state = TradeProtocolState.AWAITING_CONFIRM;
                        cur.lastActionTimeMs = now;
                        return cur;
                    });
                    return BehaviorStatus.EXECUTED;

                case APPROVING:
                    RouterTradePackets.sendTradeApprove(context);
                    pendingTrades.computeIfPresent(sessionId, (id, cur) -> {
                        cur.state = TradeProtocolState.AWAITING_APPROVE;
                        cur.lastActionTimeMs = now;
                        return cur;
                    });
                    return BehaviorStatus.EXECUTED;

                default:
                    return BehaviorStatus.SKIPPED;
            }
        }
        return BehaviorStatus.SKIPPED;
    }
}
