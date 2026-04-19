package org.sokybot.behaviors.town.internal.strategies;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.sokybot.town.api.DeathRecoveryAction;
import org.sokybot.town.api.DeathRecoveryActionKind;
import org.sokybot.town.api.IDeathRecoveryStrategy;
import org.sokybot.town.api.ITownPolicy;
import org.sokybot.town.api.ITownSnapshot;

@Component(service = IDeathRecoveryStrategy.class, property = "service.ranking:Integer=0")
public final class DefaultDeathRecoveryStrategy implements IDeathRecoveryStrategy {

    @Override
    public Optional<DeathRecoveryAction> nextStep(ITownSnapshot snapshot, ITownPolicy policy) {
        if (snapshot == null || policy == null || !snapshot.isDead()) {
            return Optional.empty();
        }
        return Optional.of(DeathRecoveryAction.builder()
                .kind(DeathRecoveryActionKind.RESPAWN_AT_TOWN)
                .putParameter("phase", "respawn")
                .build());
    }
}
