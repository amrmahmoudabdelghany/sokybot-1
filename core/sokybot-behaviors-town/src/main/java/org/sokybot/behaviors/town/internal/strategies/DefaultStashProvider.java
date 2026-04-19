package org.sokybot.behaviors.town.internal.strategies;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.sokybot.town.api.IInventorySnapshot;
import org.sokybot.town.api.IStashProvider;
import org.sokybot.town.api.ITownPolicy;
import org.sokybot.town.api.StashOrder;

@Component(service = IStashProvider.class, property = "service.ranking:Integer=0")
public final class DefaultStashProvider implements IStashProvider {

    @Override
    public Optional<StashOrder> needs(IInventorySnapshot inventory, ITownPolicy policy) {
        if (inventory == null || policy == null || !policy.isStashOverflowLoot()) {
            return Optional.empty();
        }
        int minFree = policy.getMinFreeInventorySlots();
        if (inventory.getFreeSlots() > minFree && inventory.getGold() < policy.getBankGoldThreshold()) {
            return Optional.empty();
        }
        StashOrder.Builder b = StashOrder.builder();
        boolean any = false;
        for (org.sokybot.town.api.ItemStackSnapshot stack : inventory.listStacks()) {
            if (stack.getSlotIndex() >= 13 && stack.getQuantity() > 0) {
                b.addSlotIndex(stack.getSlotIndex());
                any = true;
            }
        }
        return any ? Optional.of(b.build()) : Optional.empty();
    }
}
