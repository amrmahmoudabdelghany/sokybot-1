package org.sokybot.behaviors.town.internal.strategies;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.sokybot.town.api.IInventorySnapshot;
import org.sokybot.town.api.IRestockProvider;
import org.sokybot.town.api.ITownPolicy;
import org.sokybot.town.api.RestockItem;
import org.sokybot.town.api.RestockOrder;

@Component(service = IRestockProvider.class, property = "service.ranking:Integer=0")
public final class DefaultRestockProvider implements IRestockProvider {

    @Override
    public Optional<RestockOrder> needs(IInventorySnapshot inventory, ITownPolicy policy) {
        if (inventory == null || policy == null) {
            return Optional.empty();
        }
        RestockOrder.Builder b = RestockOrder.builder();
        boolean any = false;
        int hpId = policy.getHpPotionItemRefId();
        if (hpId > 0) {
            int have = inventory.countItemRef(hpId);
            int want = policy.getHpPotionTargetQuantity();
            if (have < want) {
                b.addLine(RestockItem.builder().itemRefId(hpId).targetQuantity(want - have).build());
                any = true;
            }
        }
        int mpId = policy.getMpPotionItemRefId();
        if (mpId > 0) {
            int have = inventory.countItemRef(mpId);
            int want = policy.getMpPotionTargetQuantity();
            if (have < want) {
                b.addLine(RestockItem.builder().itemRefId(mpId).targetQuantity(want - have).build());
                any = true;
            }
        }
        for (RestockItem row : policy.getExtraRestockTargets()) {
            int have = inventory.countItemRef(row.getItemRefId());
            if (have < row.getTargetQuantity()) {
                b.addLine(RestockItem.builder()
                        .itemRefId(row.getItemRefId())
                        .targetQuantity(row.getTargetQuantity() - have)
                        .build());
                any = true;
            }
        }
        return any ? Optional.of(b.build()) : Optional.empty();
    }
}
