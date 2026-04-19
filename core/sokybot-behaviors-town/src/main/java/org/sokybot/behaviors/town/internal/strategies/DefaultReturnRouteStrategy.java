package org.sokybot.behaviors.town.internal.strategies;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.sokybot.town.api.IReturnRouteStrategy;
import org.sokybot.town.api.ITownPolicy;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.api.ReturnAction;
import org.sokybot.town.api.ReturnActionKind;

@Component(service = IReturnRouteStrategy.class, property = "service.ranking:Integer=0")
public final class DefaultReturnRouteStrategy implements IReturnRouteStrategy {

    @Override
    public Optional<ReturnAction> nextStep(ITownSnapshot snapshot, ITownPolicy policy) {
        if (snapshot == null || policy == null || snapshot.isDead()) {
            return Optional.empty();
        }
        if (snapshot.getInventory().getFreeSlots() <= policy.getMinFreeInventorySlots()) {
            return Optional.empty();
        }
        if (policy.isTravelScriptEnabled() && policy.getTravelScriptId() != null
                && !policy.getTravelScriptId().isEmpty()) {
            return Optional.of(ReturnAction.builder()
                    .kind(ReturnActionKind.FOLLOW_RECORDED_ROUTE)
                    .putParameter("scriptId", policy.getTravelScriptId())
                    .build());
        }
        return Optional.of(ReturnAction.builder()
                .kind(ReturnActionKind.WALK_TO_GATE)
                .putParameter("hint", "leave-town")
                .build());
    }
}
