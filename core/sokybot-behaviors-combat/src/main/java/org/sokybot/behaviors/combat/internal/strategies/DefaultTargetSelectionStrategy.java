package org.sokybot.behaviors.combat.internal.strategies;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.sokybot.combat.api.ICombatPolicy;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.api.ITargetSelectionStrategy;
import org.sokybot.combat.api.MonsterRef;

@Component(service = ITargetSelectionStrategy.class, immediate = true, property = {
        "service.ranking:Integer=0"
})
public final class DefaultTargetSelectionStrategy implements ITargetSelectionStrategy {

    @Override
    public Optional<Integer> selectTarget(ICombatSnapshot snapshot, ICombatPolicy policy) {
        if (snapshot == null || policy == null) {
            return Optional.empty();
        }
        float maxDist = policy.getMaxEngageDistance();
        List<Integer> allow = policy.getMobRefIdAllowList();
        List<Integer> block = policy.getMobRefIdBlockList();

        List<MonsterRef> candidates = new ArrayList<>();
        for (MonsterRef m : snapshot.getNearbyMonsters()) {
            if (m.getDistanceToSelf() > maxDist || Float.isInfinite(m.getDistanceToSelf())) {
                continue;
            }
            if (!allow.isEmpty() && !allow.contains(Integer.valueOf(m.getRefObjId()))) {
                continue;
            }
            if (!block.isEmpty() && block.contains(Integer.valueOf(m.getRefObjId()))) {
                continue;
            }
            if (m.getHpPercentOrNegativeIfUnknown() == 0) {
                continue;
            }
            candidates.add(m);
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        candidates.sort(Comparator
                .comparing((MonsterRef m) -> !m.isAggressiveTowardSelf())
                .thenComparingDouble(MonsterRef::getDistanceToSelf));

        return Optional.of(Integer.valueOf(candidates.get(0).getEntityId()));
    }
}
