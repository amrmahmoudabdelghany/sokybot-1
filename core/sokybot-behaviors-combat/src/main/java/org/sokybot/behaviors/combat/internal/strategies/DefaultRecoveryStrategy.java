package org.sokybot.behaviors.combat.internal.strategies;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.sokybot.combat.api.ICombatPolicy;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.api.IRecoveryStrategy;
import org.sokybot.combat.api.RecoveryAction;
import org.sokybot.combat.api.RecoveryActionKind;

@Component(service = IRecoveryStrategy.class, immediate = true, property = {
        "service.ranking:Integer=0"
})
public final class DefaultRecoveryStrategy implements IRecoveryStrategy {

    @Override
    public Optional<RecoveryAction> nextRecovery(ICombatSnapshot snapshot, ICombatPolicy policy) {
        if (snapshot == null || policy == null) {
            return Optional.empty();
        }
        int maxHp = Math.max(1, snapshot.getMaxHp());
        int maxMp = Math.max(1, snapshot.getMaxMp());
        int hpPct = (int) ((100L * snapshot.getCurrentHp()) / maxHp);
        int mpPct = (int) ((100L * snapshot.getCurrentMp()) / maxMp);

        if (hpPct < policy.getHpPotionThresholdPercent()) {
            return Optional.of(new RecoveryAction(RecoveryActionKind.HP_POTION, policy.getHpPotionItemRefId(), null));
        }
        if (mpPct < policy.getMpPotionThresholdPercent()) {
            return Optional.of(new RecoveryAction(RecoveryActionKind.MP_POTION, policy.getMpPotionItemRefId(), null));
        }
        return Optional.empty();
    }
}
