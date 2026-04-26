package org.sokybot.behaviors.trade.internal;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.trade.internal.settings.FarmerTradeSettings;
import org.sokybot.behaviors.trade.packets.TradeEventAwait;
import org.sokybot.behaviors.trade.packets.TradePackets;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.town.api.ItemStackSnapshot;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;
import org.sokybot.trade.coordination.api.ITradeCoordinator;
import org.sokybot.trade.coordination.api.SwarmRole;
import org.sokybot.trade.coordination.api.SwarmSessionPhase;
import org.sokybot.trade.coordination.api.SwarmSessionState;
import org.sokybot.trade.events.TradeItemAdded;
import org.sokybot.trade.projections.api.ITradeModel;
import org.sokybot.trade.projections.api.TradeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fills the exchange offer from inventory; swarm farmer path is one inventory slot per tick in {@code FILLING} phase.
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class FillTradeWindowBehavior implements IBehavior<FarmerTradeSettings> {

    private static final Logger log = LoggerFactory.getLogger(FillTradeWindowBehavior.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IReactiveEventBus reactiveEventBus;

    @Override
    public String id() {
        return "fill-trade-window";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return "trade-cycle".equals(cycleId);
    }

    @Override
    public Class<FarmerTradeSettings> settingsType() {
        return FarmerTradeSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, FarmerTradeSettings settings) {
        if (settings == null) {
            return false;
        }
        Optional<ITradeCoordinator> coord = context.getServiceOptional(ITradeCoordinator.class);
        if (!coord.isPresent()) {
            return false;
        }
        Optional<SwarmSessionState> sm = coord.get().getSwarmSession(context.getMachineId());
        boolean swarmFarmerFill = sm.isPresent()
                && sm.get().getLocalRole() == SwarmRole.FARMER
                && sm.get().getPhase() == SwarmSessionPhase.FILLING;
        boolean stallFill = context.getPersistentData().containsKey(TradeKeys.COORDINATION_SESSION_ID);
        return swarmFarmerFill || stallFill;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, FarmerTradeSettings settings) {
        ITradeCoordinator coordinator = context.getService(ITradeCoordinator.class);
        Optional<SwarmSessionState> swarm = coordinator.getSwarmSession(context.getMachineId());
        boolean swarmFarmer = swarm.isPresent()
                && swarm.get().getLocalRole() == SwarmRole.FARMER
                && swarm.get().getPhase() == SwarmSessionPhase.FILLING;

        if (swarmFarmer) {
            return executeSwarmFarmerFill(context, coordinator);
        }

        return executeStallAwareFill(context);
    }

    private BehaviorStatus executeStallAwareFill(IWorkflowContext context) {
        try {
            context.getService(ITradeModel.class).getSnapshot(context.getMachineId());
            context.log("DEBUG", "fill-trade-window (stall) snapshot check for {}", context.getMachineId());
            return BehaviorStatus.EXECUTED;
        } catch (RuntimeException ex) {
            log.debug("fill-trade-window skipped: {}", ex.getMessage());
            return BehaviorStatus.SKIPPED;
        }
    }

    private BehaviorStatus executeSwarmFarmerFill(IWorkflowContext context, ITradeCoordinator coordinator) {
        if (townModel == null || reactiveEventBus == null) {
            return BehaviorStatus.SKIPPED;
        }
        Optional<ITownSnapshot> snap = townModel.snapshot(context.getMachineId());
        if (!snap.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }

        TradeSnapshot tradeSnap = context.getService(ITradeModel.class)
                .getSnapshot(context.getMachineId())
                .orElse(null);
        if (tradeSnap == null || !tradeSnap.isExchangeActive()) {
            return BehaviorStatus.SKIPPED;
        }

        int exchangeId = Math.max(0, tradeSnap.getExchangeId());
        int invCursor = intFrom(context.getPersistentData().get(TradeKeys.SWARM_FILL_SLOT_CURSOR));
        int offerSlot = intFrom(context.getPersistentData().get(TradeKeys.SWARM_OFFER_SLOT_CURSOR));
        if (offerSlot >= 12) {
            TradePackets.sendExchangeApprove(context);
            coordinator.updateSwarmSessionPhase(context.getMachineId(), SwarmSessionPhase.ACCEPTED);
            context.getPersistentData().remove(TradeKeys.SWARM_FILL_SLOT_CURSOR);
            context.getPersistentData().remove(TradeKeys.SWARM_OFFER_SLOT_CURSOR);
            return BehaviorStatus.EXECUTED;
        }

        List<ItemStackSnapshot> stacks = snap.get().getInventory().listStacks();
        ItemStackSnapshot chosen = null;
        for (ItemStackSnapshot st : stacks) {
            if (st.getSlotIndex() >= invCursor && st.getQuantity() > 0 && st.getItemRefId() > 0) {
                chosen = st;
                break;
            }
        }

        if (chosen == null) {
            TradePackets.sendExchangeApprove(context);
            coordinator.updateSwarmSessionPhase(context.getMachineId(), SwarmSessionPhase.ACCEPTED);
            context.getPersistentData().remove(TradeKeys.SWARM_FILL_SLOT_CURSOR);
            context.getPersistentData().remove(TradeKeys.SWARM_OFFER_SLOT_CURSOR);
            return BehaviorStatus.EXECUTED;
        }

        int qty = Math.min(chosen.getQuantity(), 0xFFFF);
        TradePackets.sendExchangeAddItem(context, exchangeId, chosen.getSlotIndex(), offerSlot, qty);

        TradeItemAdded ack = TradeEventAwait.awaitNext(
                reactiveEventBus,
                TradeItemAdded.class,
                e -> context.getMachineId().equals(e.getFullName())
                        && e.isSelfOffer()
                        && e.getSlot() == (byte) offerSlot,
                Duration.ofSeconds(3));

        if (ack == null) {
            log.debug("fill-trade-window: no ack for slot {} on {}", offerSlot, context.getMachineId());
            return BehaviorStatus.EXECUTED;
        }

        context.getPersistentData().put(TradeKeys.SWARM_FILL_SLOT_CURSOR, chosen.getSlotIndex() + 1);
        context.getPersistentData().put(TradeKeys.SWARM_OFFER_SLOT_CURSOR, offerSlot + 1);
        return BehaviorStatus.EXECUTED;
    }

    private static int intFrom(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return 0;
    }

    @Override
    public long postDelayMs() {
        return 300L;
    }
}
