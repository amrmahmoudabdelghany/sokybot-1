package org.sokybot.behaviors.town.internal.strategies;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.sokybot.town.api.IDurabilitySnapshot;
import org.sokybot.town.api.IRepairProvider;
import org.sokybot.town.api.ITownPolicy;
import org.sokybot.town.api.EquipDurability;
import org.sokybot.town.api.EquipSlot;
import org.sokybot.town.api.RepairOrder;

@Component(service = IRepairProvider.class, property = "service.ranking:Integer=0")
public final class DefaultRepairProvider implements IRepairProvider {

    @Override
    public Optional<RepairOrder> needs(IDurabilitySnapshot durability, ITownPolicy policy) {
        if (durability == null || policy == null) {
            return Optional.empty();
        }
        int thr = policy.getRepairDurabilityThresholdPercent();
        RepairOrder.Builder b = RepairOrder.builder();
        boolean any = false;
        for (EquipSlot slot : EquipSlot.values()) {
            Optional<Integer> pct = durability.getDurabilityPercent(slot);
            if (pct.isPresent() && pct.get().intValue() <= thr) {
                b.addSlot(EquipDurability.builder().slot(slot).durabilityPercent(pct.get().intValue()).build());
                any = true;
            }
        }
        return any ? Optional.of(b.build()) : Optional.empty();
    }
}
