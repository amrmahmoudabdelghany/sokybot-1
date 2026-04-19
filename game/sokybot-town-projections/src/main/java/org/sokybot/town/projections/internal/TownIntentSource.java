package org.sokybot.town.projections.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.town.api.IIntent;
import org.sokybot.town.api.IIntentSource;
import org.sokybot.town.api.Intent;
import org.sokybot.town.api.IntentKind;
import org.sokybot.town.api.ITownPolicy;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.api.IDurabilitySnapshot;
import org.sokybot.town.api.EquipSlot;
import org.sokybot.town.api.TownPolicy;
import org.sokybot.town.projections.api.ITownModel;

/**
 * Declares {@link IntentKind#TOWN} when logistics projections detect inventory pressure or gear failure.
 */
@Component(service = IIntentSource.class)
public final class TownIntentSource implements IIntentSource {

    private final ITownPolicy defaults = TownPolicy.builder().build();

    @Reference
    private ITownModel townModel;

    @Override
    public Optional<IIntent> currentIntent(String machineFullName) {
        if (!defaults.isTownLoopEnabled()) {
            return Optional.empty();
        }
        Optional<ITownSnapshot> snapOpt = townModel.snapshot(machineFullName);
        if (!snapOpt.isPresent()) {
            return Optional.empty();
        }
        ITownSnapshot snap = snapOpt.get();
        String reason = evaluate(snap, defaults);
        if (reason == null) {
            return Optional.empty();
        }
        return Optional.of(Intent.builder()
                .kind(IntentKind.TOWN)
                .reason(reason)
                .declaredAtEpochMillis(System.currentTimeMillis())
                .machineFullName(machineFullName)
                .putMetadata("source", "TownIntentSource")
                .build());
    }

    static String evaluate(ITownSnapshot snap, ITownPolicy policy) {
        if (snap.getInventory().getFreeSlots() <= 0) {
            return "inventory-full";
        }
        if (potionsCritical(snap, policy)) {
            return "potions-empty";
        }
        if (durabilityCritical(snap.getDurability(), policy)) {
            return "durability-low";
        }
        return null;
    }

    private static boolean potionsCritical(ITownSnapshot snap, ITownPolicy policy) {
        int hpRef = policy.getHpPotionItemRefId();
        if (hpRef > 0 && snap.getInventory().countItemRef(hpRef) <= 0) {
            return true;
        }
        int mpRef = policy.getMpPotionItemRefId();
        return mpRef > 0 && snap.getInventory().countItemRef(mpRef) <= 0;
    }

    private static boolean durabilityCritical(IDurabilitySnapshot dur, ITownPolicy policy) {
        int threshold = policy.getRepairDurabilityThresholdPercent();
        for (EquipSlot slot : EquipSlot.values()) {
            Optional<Integer> pct = dur.getDurabilityPercent(slot);
            if (pct.isPresent() && pct.get().intValue() <= threshold) {
                return true;
            }
        }
        return false;
    }
}
