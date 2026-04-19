package org.sokybot.behaviors.town.internal;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.town.internal.settings.TownSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.storage.api.IStorageModel;
import org.sokybot.storage.api.IStorageSnapshot;
import org.sokybot.storage.api.StorageCycleKeys;
import org.sokybot.storage.api.StorageType;
import org.sokybot.town.api.INpcInteractionFacade;
import org.sokybot.town.api.ITownPolicy;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.api.IWithdrawProvider;
import org.sokybot.town.api.NpcRole;
import org.sokybot.town.api.TownCycleKeys;
import org.sokybot.town.api.VendorRef;
import org.sokybot.town.api.WithdrawOrder;
import org.sokybot.town.projections.api.ITownDirectory;
import org.sokybot.town.projections.api.ITownModel;

@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class WithdrawConsumableBehavior implements IBehavior<TownSettings> {

    @Reference
    private ITownModel townModel;

    @Reference
    private ITownDirectory townDirectory;

    @Reference
    private IWithdrawProvider withdrawProvider;

    @Reference
    private INpcInteractionFacade npcFacade;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IStorageModel storageModel;

    @Override
    public String id() {
        return StorageCycleKeys.BEHAVIOR_WITHDRAW_CONSUMABLE;
    }

    @Override
    public int order() {
        return 35;
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
        ITownPolicy policy = settings.toPolicy();
        if (!policy.isWithdrawFromStorageEnabled() || storageModel == null) {
            return false;
        }
        Optional<ITownSnapshot> snap = townModel.snapshot(context.getMachineId());
        if (!snap.isPresent() || snap.get().isDead()) {
            return false;
        }
        Optional<IStorageSnapshot> sto = storageModel.personal(context.getMachineId());
        return sto.isPresent() && withdrawProvider
                .next(snap.get().getInventory(), sto.get(), policy).isPresent();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, TownSettings settings) {
        Optional<ITownSnapshot> snap = townModel.snapshot(context.getMachineId());
        if (!snap.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        ITownPolicy policy = settings.toPolicy();
        if (storageModel == null) {
            return BehaviorStatus.SKIPPED;
        }
        Optional<IStorageSnapshot> personal = storageModel.personal(context.getMachineId());
        if (!personal.isPresent()) {
            context.getPersistentData().remove(StorageCycleKeys.KEY_ACTIVE_WITHDRAW_QUEUE);
            return BehaviorStatus.SKIPPED;
        }
        Optional<WithdrawOrder> next = withdrawProvider.next(snap.get().getInventory(), personal.get(), policy);
        if (!next.isPresent()) {
            context.getPersistentData().remove(StorageCycleKeys.KEY_ACTIVE_WITHDRAW_QUEUE);
            return BehaviorStatus.SKIPPED;
        }
        Optional<VendorRef> resolved = resolveStorageVendor(context);
        VendorRef vendor = next.get().getPreferredStorageNpc().orElse(null);
        if (vendor == null) {
            vendor = resolved.orElse(null);
        }
        if (vendor == null) {
            return BehaviorStatus.SKIPPED;
        }
        if (!storageModel.isStorageFresh(context.getMachineId(), StorageType.PERSONAL,
                policy.getStorageOpenStaleAfterMs())) {
            try {
                npcFacade.openStorage(context, vendor).get(20, TimeUnit.SECONDS);
            } catch (Exception ex) {
                context.log("WARN", "Town storage open failed: {}", ex.toString());
                return BehaviorStatus.SKIPPED;
            }
            context.getPersistentData().put(StorageCycleKeys.KEY_LAST_STORAGE_OPEN_AT_MS,
                    System.currentTimeMillis());
            return BehaviorStatus.EXECUTED;
        }
        WithdrawOrder w = next.get();
        try {
            npcFacade.withdraw(context, vendor, w.getStorageSlotIndex(), w.getQuantity()).get(20, TimeUnit.SECONDS);
        } catch (Exception ex) {
            context.log("WARN", "Town withdraw failed: {}", ex.toString());
            return BehaviorStatus.SKIPPED;
        }
        context.getPersistentData().put(StorageCycleKeys.KEY_ACTIVE_WITHDRAW_QUEUE, w);
        return BehaviorStatus.EXECUTED;
    }

    private Optional<VendorRef> resolveStorageVendor(IWorkflowContext context) {
        return townDirectory.nearestTown(context.getMachineId())
                .filter(n -> n.getRole() == NpcRole.STORAGE)
                .map(n -> VendorRef.fromNpc(n, "storage"));
    }
}
