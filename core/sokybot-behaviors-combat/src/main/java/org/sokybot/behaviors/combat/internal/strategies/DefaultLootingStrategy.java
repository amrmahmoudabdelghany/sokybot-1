package org.sokybot.behaviors.combat.internal.strategies;

import java.util.List;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.sokybot.combat.api.DroppedItemRef;
import org.sokybot.combat.api.ICombatPolicy;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.api.ILootingStrategy;

@Component(service = ILootingStrategy.class, immediate = true, property = {
        "service.ranking:Integer=0"
})
public final class DefaultLootingStrategy implements ILootingStrategy {

    @Override
    public Optional<DroppedItemRef> pickNext(ICombatSnapshot snapshot, ICombatPolicy policy) {
        if (snapshot == null || policy == null) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        float maxPick = policy.getLootRadius();
        List<Integer> white = policy.getLootItemRefIdWhitelist();

        DroppedItemRef best = null;
        float bestDist = Float.MAX_VALUE;

        for (DroppedItemRef d : snapshot.getNearbyLoot()) {
            if (d.getDistanceToSelf() > maxPick || Float.isInfinite(d.getDistanceToSelf())) {
                continue;
            }
            if (d.getOwnerEntityId() != null && d.getOwnerExpiresAtEpochMs() > now) {
                continue;
            }
            if (!white.isEmpty() && !white.contains(Integer.valueOf(d.getItemRefId()))) {
                continue;
            }
            if (best == null || d.getDistanceToSelf() < bestDist) {
                best = d;
                bestDist = d.getDistanceToSelf();
            }
        }
        return Optional.ofNullable(best);
    }
}
