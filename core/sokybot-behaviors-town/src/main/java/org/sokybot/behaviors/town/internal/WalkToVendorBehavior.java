package org.sokybot.behaviors.town.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.town.internal.settings.TownSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.town.api.IRepairProvider;
import org.sokybot.town.api.IRestockProvider;
import org.sokybot.town.api.IStashProvider;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.api.TownCycleKeys;
import org.sokybot.town.projections.api.ITownDirectory;
import org.sokybot.town.projections.api.ITownModel;

@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class WalkToVendorBehavior implements IBehavior<TownSettings> {

    @Reference
    private ITownModel townModel;

    @Reference
    private ITownDirectory townDirectory;

    @Reference
    private IRestockProvider restockProvider;

    @Reference
    private IRepairProvider repairProvider;

    @Reference
    private IStashProvider stashProvider;

    @Override
    public String id() {
        return TownCycleKeys.BEHAVIOR_WALK_TO_VENDOR;
    }

    @Override
    public int order() {
        return 20;
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
        var pol = settings.toPolicy();
        boolean logistics = restockProvider.needs(snap.get().getInventory(), pol).isPresent()
                || repairProvider.needs(snap.get().getDurability(), pol).isPresent()
                || stashProvider.needs(snap.get().getInventory(), pol).isPresent();
        return logistics && townDirectory.nearestVendor(context.getMachineId()).isPresent();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, TownSettings settings) {
        townDirectory.nearestVendor(context.getMachineId())
                .ifPresent(npc -> context.log("INFO", "Town walk-to-vendor toward {}", npc.getDisplayName()));
        return BehaviorStatus.EXECUTED;
    }
}
