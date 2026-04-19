package org.sokybot.town.projections.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.town.api.IIntent;
import org.sokybot.town.api.IIntentSource;
import org.sokybot.town.api.Intent;
import org.sokybot.town.api.IntentKind;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;

/**
 * Declares {@link IntentKind#DEATH} while the tactical overlay reports the player dead.
 */
@Component(service = IIntentSource.class)
public final class DeathIntentSource implements IIntentSource {

    @Reference
    private ITownModel townModel;

    @Override
    public Optional<IIntent> currentIntent(String machineFullName) {
        Optional<ITownSnapshot> snapOpt = townModel.snapshot(machineFullName);
        if (!snapOpt.isPresent()) {
            return Optional.empty();
        }
        ITownSnapshot snap = snapOpt.get();
        if (!snap.isDead()) {
            return Optional.empty();
        }
        return Optional.of(Intent.builder()
                .kind(IntentKind.DEATH)
                .reason("player-dead")
                .declaredAtEpochMillis(System.currentTimeMillis())
                .machineFullName(machineFullName)
                .putMetadata("source", "DeathIntentSource")
                .build());
    }
}
