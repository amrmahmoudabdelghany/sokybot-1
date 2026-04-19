package org.sokybot.behaviors.combat.internal.strategies;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.sokybot.combat.api.ICombatPolicy;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.api.ISkillRotation;
import org.sokybot.combat.api.MonsterRef;
import org.sokybot.combat.api.SkillAction;
import org.sokybot.combat.api.SkillActionKind;

@Component(service = ISkillRotation.class, immediate = true, property = {
        "service.ranking:Integer=0"
})
public final class DefaultSkillRotation implements ISkillRotation {

    @Override
    public Optional<SkillAction> nextAction(ICombatSnapshot snapshot, ICombatPolicy policy) {
        if (snapshot == null || policy == null) {
            return Optional.empty();
        }
        Optional<Integer> tid = snapshot.getCurrentTargetEntityId();
        if (!tid.isPresent()) {
            return Optional.empty();
        }
        int targetId = tid.get().intValue();
        boolean alive = false;
        for (MonsterRef m : snapshot.getNearbyMonsters()) {
            if (m.getEntityId() == targetId && m.getHpPercentOrNegativeIfUnknown() != 0) {
                alive = true;
                break;
            }
        }
        if (!alive) {
            return Optional.empty();
        }
        return Optional.of(new SkillAction(SkillActionKind.AUTO_ATTACK, 0, targetId, null));
    }
}
