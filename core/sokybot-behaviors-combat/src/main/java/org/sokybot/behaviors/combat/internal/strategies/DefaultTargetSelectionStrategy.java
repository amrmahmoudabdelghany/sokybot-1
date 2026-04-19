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
import org.sokybot.combat.api.MonsterTier;

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
        List<Integer> party = policy.getPartyMemberEntityIds();

        Optional<Integer> selfOpt = snapshot.getSelfEntityId();
        Integer selfId = selfOpt.orElse(null);

        List<MonsterRef> candidates = new ArrayList<>();
        for (MonsterRef m : snapshot.getNearbyMonsters()) {
            if (m.getDistanceToSelf() > maxDist || Float.isInfinite(m.getDistanceToSelf())) {
                continue;
            }

            Optional<Float> fromAnchor = m.getDistanceFromTrainingAnchor();
            if (fromAnchor.isPresent()) {
                if (fromAnchor.get().floatValue() > policy.getLeashRadius()) {
                    continue;
                }
            } else {
                if (m.getDistanceToSelf() > policy.getLeashRadius()) {
                    continue;
                }
            }

            Optional<Integer> owner = m.getFirstAttackerEntityId();
            if (owner.isPresent()) {
                int attacker = owner.get().intValue();
                if (selfId == null) {
                    continue;
                }
                if (attacker != selfId.intValue() && !party.contains(Integer.valueOf(attacker))) {
                    continue;
                }
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
            MonsterTier tier = m.getTier();
            if (policy.isAvoidGiants() && tier == MonsterTier.GIANT) {
                continue;
            }
            if (policy.isAvoidUniques() && tier == MonsterTier.UNIQUE) {
                continue;
            }
            if (policy.isAvoidChampions() && tier == MonsterTier.CHAMPION) {
                continue;
            }
            if (policy.isAvoidTitans() && tier == MonsterTier.TITAN) {
                continue;
            }
            if (policy.isAvoidPartyMobs() && tier == MonsterTier.PARTY) {
                continue;
            }
            if (policy.isAvoidQuestMobs() && tier == MonsterTier.QUEST) {
                continue;
            }
            candidates.add(m);
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        candidates.sort(Comparator
                .comparing((MonsterRef mr) -> !mr.isAggressiveTowardSelf())
                .thenComparingDouble(MonsterRef::getDistanceToSelf));

        return Optional.of(Integer.valueOf(candidates.get(0).getEntityId()));
    }
}
