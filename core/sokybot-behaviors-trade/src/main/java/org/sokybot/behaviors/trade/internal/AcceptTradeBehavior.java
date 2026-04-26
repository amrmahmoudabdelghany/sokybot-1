package org.sokybot.behaviors.trade.internal;

import java.time.Duration;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.trade.internal.settings.MuleStallSettings;
import org.sokybot.behaviors.trade.packets.TradeEventAwait;
import org.sokybot.behaviors.trade.packets.TradePackets;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.trade.events.TradeWindowOpened;
import org.sokybot.trade.projections.api.ITradeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mule-side stall / exchange: confirms an incoming trade request and waits for the trade window.
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class AcceptTradeBehavior implements IBehavior<MuleStallSettings> {

    private static final Logger log = LoggerFactory.getLogger(AcceptTradeBehavior.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITradeModel tradeModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IReactiveEventBus reactiveEventBus;

    @Override
    public String id() {
        return "accept-trade";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return "stall-cycle".equals(cycleId);
    }

    @Override
    public Class<MuleStallSettings> settingsType() {
        return MuleStallSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, MuleStallSettings settings) {
        if (settings == null) {
            return false;
        }
        return !Boolean.TRUE.equals(context.getPersistentData().get(TradeKeys.STALL_TRADE_WINDOW_OPENED));
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, MuleStallSettings settings) {
        try {
            if (tradeModel == null || reactiveEventBus == null) {
                return BehaviorStatus.SKIPPED;
            }
            if (context.getPersistentData().putIfAbsent(TradeKeys.STALL_EXCHANGE_CONFIRM_SENT, Boolean.TRUE) != null) {
                return BehaviorStatus.EXECUTED;
            }
            TradePackets.sendExchangeConfirm(context);
            TradeWindowOpened opened = TradeEventAwait.awaitNext(
                    reactiveEventBus,
                    TradeWindowOpened.class,
                    e -> context.getMachineId().equals(e.getFullName()),
                    Duration.ofSeconds(5));
            if (opened == null) {
                context.getPersistentData().remove(TradeKeys.STALL_EXCHANGE_CONFIRM_SENT);
                log.debug("accept-trade: no window opened for {}", context.getMachineId());
            } else {
                context.getPersistentData().put(TradeKeys.STALL_TRADE_WINDOW_OPENED, Boolean.TRUE);
            }
            return BehaviorStatus.EXECUTED;
        } catch (RuntimeException ex) {
            log.debug("accept-trade: {}", ex.getMessage());
            return BehaviorStatus.SKIPPED;
        }
    }

    @Override
    public long postDelayMs() {
        return 400L;
    }
}
