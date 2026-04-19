package org.sokybot.behaviors.town.internal.strategies;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.sokybot.storage.api.IStorageSnapshot;
import org.sokybot.storage.api.StorageStack;
import org.sokybot.town.api.IInventorySnapshot;
import org.sokybot.town.api.ITownPolicy;
import org.sokybot.town.api.IWithdrawProvider;
import org.sokybot.town.api.RestockItem;
import org.sokybot.town.api.WithdrawOrder;

@Component(service = IWithdrawProvider.class, property = "service.ranking:Integer=0")
public final class DefaultWithdrawProvider implements IWithdrawProvider {

    @Override
    public Optional<WithdrawOrder> next(IInventorySnapshot inventory, IStorageSnapshot personalStorage,
            ITownPolicy policy) {
        if (inventory == null || personalStorage == null || policy == null) {
            return Optional.empty();
        }
        for (RestockItem row : policy.getExtraRestockTargets()) {
            Optional<WithdrawOrder> o = plan(row.getItemRefId(), row.getTargetQuantity(), personalStorage,
                    inventory);
            if (o.isPresent()) {
                return o;
            }
        }
        if (policy.getHpPotionItemRefId() != 0) {
            Optional<WithdrawOrder> o = plan(policy.getHpPotionItemRefId(), policy.getHpPotionTargetQuantity(),
                    personalStorage, inventory);
            if (o.isPresent()) {
                return o;
            }
        }
        if (policy.getMpPotionItemRefId() != 0) {
            return plan(policy.getMpPotionItemRefId(), policy.getMpPotionTargetQuantity(), personalStorage,
                    inventory);
        }
        return Optional.empty();
    }

    private static Optional<WithdrawOrder> plan(int itemRefId, int targetQuantity, IStorageSnapshot storage,
            IInventorySnapshot inventory) {
        int needed = targetQuantity - inventory.countItemRef(itemRefId);
        if (needed <= 0) {
            return Optional.empty();
        }
        StorageStack best = null;
        for (StorageStack s : storage.getStacks()) {
            if (s.getItemRefId() == itemRefId && s.getQuantity() > 0) {
                if (best == null || s.getQuantity() > best.getQuantity()) {
                    best = s;
                }
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        int qty = Math.min(needed, best.getQuantity());
        return Optional.of(WithdrawOrder.builder()
                .storageSlotIndex(best.getSlotIndex())
                .quantity(qty)
                .targetItemRefId(itemRefId)
                .build());
    }
}
