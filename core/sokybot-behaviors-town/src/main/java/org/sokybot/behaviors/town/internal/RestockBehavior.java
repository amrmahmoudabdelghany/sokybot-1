package org.sokybot.behaviors.town.internal;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.town.internal.settings.TownSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.town.api.INpcInteractionFacade;
import org.sokybot.town.api.IRestockProvider;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.api.NpcRef;
import org.sokybot.town.api.RestockItem;
import org.sokybot.town.api.RestockOrder;
import org.sokybot.town.api.TownCycleKeys;
import org.sokybot.town.api.VendorRef;
import org.sokybot.town.projections.api.ITownDirectory;
import org.sokybot.town.projections.api.ITownModel;

@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class RestockBehavior implements IBehavior<TownSettings> {

    @Reference
    private ITownModel townModel;

    @Reference
    private ITownDirectory townDirectory;

    @Reference
    private IRestockProvider restockProvider;

    @Reference
    private INpcInteractionFacade npcFacade;

    @Override
    public String id() {
        return TownCycleKeys.BEHAVIOR_RESTOCK;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return TownCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<TownSettings> settingsType() {
        return TownSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, TownSettings settings) {
        if (settings == null || !settings.isTownLoopEnabled()) {
            return false;
        }
        Optional<ITownSnapshot> snap = townModel.snapshot(context.getMachineId());
        if (!snap.isPresent() || snap.get().isDead()) {
            return false;
        }
        return restockProvider.needs(snap.get().getInventory(), settings.toPolicy()).isPresent();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, TownSettings settings) {
        Optional<ITownSnapshot> snap = townModel.snapshot(context.getMachineId());
        if (!snap.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        Optional<RestockOrder> order = restockProvider.needs(snap.get().getInventory(), settings.toPolicy());
        if (!order.isPresent() || order.get().getLines().isEmpty()) {
            return BehaviorStatus.SKIPPED;
        }
        Optional<NpcRef> npc = townDirectory.nearestVendor(context.getMachineId());
        if (!npc.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        VendorRef vendor = VendorRef.fromNpc(npc.get(), "restock");
        RestockItem first = order.get().getLines().get(0);
        try {
            npcFacade.buy(context, vendor, first.getItemRefId(), first.getTargetQuantity())
                    .get(20, TimeUnit.SECONDS);
        } catch (Exception ex) {
            context.log("WARN", "Town restock failed: {}", ex.toString());
            return BehaviorStatus.SKIPPED;
        }
        context.getPersistentData().put(TownCycleKeys.ACTIVE_RESTOCK_ORDER, order.get());
        return BehaviorStatus.EXECUTED;
    }
}
